#!/usr/bin/env bash
#
# Fase 4 do monitoramento: liga o pg_stat_statements no Postgres de produção, o
# que dá o painel "top queries por tempo total" no Grafana.
#
# REINICIA O POSTGRES: a API devolve erro por cerca de 10 a 30 segundos. Rode
# numa janela combinada, fora do horário de uso. O script:
#   1. confere que a biblioteca existe na imagem (sem ela o Postgres não sobe);
#   2. faz backup (o mesmo do deploy);
#   3. mostra o SQL e pede "sim";
#   4. ALTER SYSTEM SET shared_preload_libraries (grava postgresql.auto.conf no
#      volume; não precisa recriar o container);
#   5. docker restart postgres-prod e espera ficar pronto;
#   6. CREATE EXTENSION pg_stat_statements;
#   7. liga o coletor no postgres-exporter.

set -Eeuo pipefail

if [ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
PG_CONTAINER="${PG_CONTAINER:-postgres-prod}"
ENV_FILE="/etc/geosegbar/monitoring/monitoring.env"
INSTALL_DIR="/opt/geosegbar-monitoring"

die() { echo "❌ $*" >&2; exit 1; }
psql_prod() { docker exec "$PG_CONTAINER" psql -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1 "$@"; }

[ "$(id -u)" -eq 0 ] || die "Rode como root."
[ -n "${DB_USERNAME:-}" ] && [ -n "${DB_NAME:-}" ] || die "DB_USERNAME e DB_NAME precisam vir do .env.prod."
docker ps -q -f "name=^${PG_CONTAINER}$" | grep -q . || die "Container $PG_CONTAINER não está rodando."

docker exec "$PG_CONTAINER" sh -c 'test -f "$(pg_config --pkglibdir)/pg_stat_statements.so"' \
  || die "pg_stat_statements.so não existe na imagem do Postgres. Nada foi alterado."

ATUAL="$(psql_prod -tAc "SHOW shared_preload_libraries" | xargs)"
JA_CARREGADA=false
case ",${ATUAL// /}," in
  *,pg_stat_statements,*) JA_CARREGADA=true ;;
esac
# shared_preload_libraries é lista: cada biblioteca vai num literal próprio.
# Um único literal 'a,pg_stat_statements' viraria UMA biblioteca com vírgula no
# nome, e o Postgres não subiria.
NOVO_SQL=""
IFS=',' read -r -a BIBLIOTECAS <<< "$ATUAL"
for lib in "${BIBLIOTECAS[@]}"; do
  lib="$(echo "$lib" | tr -d " \"'")"
  [ -n "$lib" ] || continue
  [[ "$lib" =~ ^[A-Za-z0-9_.$/-]+$ ]] || die "Nome de biblioteca inesperado em shared_preload_libraries: $lib"
  NOVO_SQL="${NOVO_SQL:+$NOVO_SQL, }'$lib'"
done
[ "$JA_CARREGADA" = true ] || NOVO_SQL="${NOVO_SQL:+$NOVO_SQL, }'pg_stat_statements'"

echo ""
echo "shared_preload_libraries hoje: '${ATUAL}'"
echo "Plano:"
echo "────────────────────────────────────────────────────────────"
if [ "$JA_CARREGADA" = true ]; then
  echo "(biblioteca já carregada: sem ALTER SYSTEM e sem reinício)"
else
  echo "ALTER SYSTEM SET shared_preload_libraries = ${NOVO_SQL};"
  echo "docker restart ${PG_CONTAINER}      ← API com erro por ~10-30 s"
fi
echo "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;   -- banco ${DB_NAME}"
echo "PG_STAT_STATEMENTS_FLAG=collector.stat_statements em ${ENV_FILE}"
echo "────────────────────────────────────────────────────────────"
read -r -p "Digite 'sim' para executar no banco de PRODUÇÃO: " resposta
[ "$resposta" = "sim" ] || { echo "Cancelado. Nada foi executado."; exit 0; }

if [ "$JA_CARREGADA" != true ]; then
  echo "💾 Backup antes de reiniciar o banco..."
  set +e
  bash "$ROOT_DIR/bash/scripts/backup_database_prod.sh"
  BACKUP_EXIT=$?
  set -e
  [ "$BACKUP_EXIT" -eq 0 ] || [ "$BACKUP_EXIT" -eq 2 ] \
    || die "Backup falhou (código $BACKUP_EXIT). Nada foi alterado no banco."

  psql_prod -c "ALTER SYSTEM SET shared_preload_libraries = ${NOVO_SQL};"

  echo "🔄 Reiniciando ${PG_CONTAINER}..."
  docker restart "$PG_CONTAINER" >/dev/null
  PRONTO=false
  for _ in $(seq 1 45); do
    if docker exec "$PG_CONTAINER" pg_isready -U "$DB_USERNAME" -d "$DB_NAME" >/dev/null 2>&1; then
      PRONTO=true
      break
    fi
    sleep 2
  done
  if [ "$PRONTO" != true ]; then
    echo "❌ O Postgres não voltou em 90 s. Log:"
    docker logs --tail 40 "$PG_CONTAINER" 2>&1 || true
    echo ""
    echo "Para desfazer: remova a linha shared_preload_libraries de postgresql.auto.conf"
    echo "no volume postgres-prod-data e rode docker start ${PG_CONTAINER}."
    echo "Passo a passo em docs/MONITORAMENTO.md, seção \"Desfazer a Fase 4\"."
    exit 1
  fi
  echo "✅ Postgres de volta."
fi

psql_prod -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"
echo "✅ Extensão pg_stat_statements criada em ${DB_NAME}."

if [ -f "$ENV_FILE" ]; then
  if grep -q '^PG_STAT_STATEMENTS_FLAG=' "$ENV_FILE"; then
    sed -i 's|^PG_STAT_STATEMENTS_FLAG=.*|PG_STAT_STATEMENTS_FLAG=collector.stat_statements|' "$ENV_FILE"
  else
    echo "PG_STAT_STATEMENTS_FLAG=collector.stat_statements" >> "$ENV_FILE"
  fi
  if [ -f "$INSTALL_DIR/docker-compose.yml" ]; then
    docker compose -p geosegbar-monitoring --env-file "$ENV_FILE" -f "$INSTALL_DIR/docker-compose.yml" \
      up -d --force-recreate postgres-exporter
    echo "✅ Coletor stat_statements ligado no postgres-exporter."
  fi
else
  echo "⚠️  $ENV_FILE não existe: ligue PG_STAT_STATEMENTS_FLAG depois do deploy do monitoramento."
fi
