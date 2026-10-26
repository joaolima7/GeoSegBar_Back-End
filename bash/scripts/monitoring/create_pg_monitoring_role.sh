#!/usr/bin/env bash
#
# Cria (ou alinha a senha de) o papel `monitoring` no Postgres de produção, usado
# pelo postgres-exporter. Papel só de leitura de estatística: membro de
# pg_monitor, sem acesso às tabelas da aplicação, no máximo 3 conexões.
#
# ESCRITA NO BANCO: o script mostra o SQL e só executa depois de você digitar
# "sim". A senha vem do arquivo gerado pelo deploy do monitoramento e não
# aparece na tela, no histórico do shell nem na lista de processos.

set -Eeuo pipefail

if [ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

SECRET_FILE="/etc/geosegbar/monitoring/secrets/pg_monitoring_password"
PG_CONTAINER="${PG_CONTAINER:-postgres-prod}"

die() { echo "❌ $*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Rode como root (a senha fica em $SECRET_FILE, só root lê)."
[ -s "$SECRET_FILE" ] || die "Senha ausente em $SECRET_FILE. Rode o deploy do monitoramento uma vez antes."
[ -n "${DB_USERNAME:-}" ] && [ -n "${DB_NAME:-}" ] || die "DB_USERNAME e DB_NAME precisam vir do .env.prod."
docker ps -q -f "name=^${PG_CONTAINER}$" | grep -q . || die "Container $PG_CONTAINER não está rodando."

SENHA="$(cat "$SECRET_FILE")"
[[ "$SENHA" =~ ^[A-Za-z0-9]{24,}$ ]] || die "A senha em $SECRET_FILE deve ter 24+ caracteres alfanuméricos."

EXISTE="$(docker exec "$PG_CONTAINER" psql -U "$DB_USERNAME" -d "$DB_NAME" -tAc \
          "SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'monitoring')")"

echo ""
echo "Banco: ${DB_NAME} (container ${PG_CONTAINER}), executando como ${DB_USERNAME}."
echo "SQL que será executado:"
echo "────────────────────────────────────────────────────────────"
if [ "$EXISTE" = "t" ]; then
  echo "ALTER ROLE monitoring WITH LOGIN PASSWORD '********' CONNECTION LIMIT 3;"
else
  echo "CREATE ROLE monitoring WITH LOGIN PASSWORD '********' CONNECTION LIMIT 3;"
fi
echo "GRANT pg_monitor TO monitoring;"
echo "────────────────────────────────────────────────────────────"
echo "(********** = conteúdo de $SECRET_FILE)"
echo ""
read -r -p "Digite 'sim' para executar no banco de PRODUÇÃO: " resposta
[ "$resposta" = "sim" ] || { echo "Cancelado. Nada foi executado."; exit 0; }

# A senha entra por stdin (\set), nunca na linha de comando.
{
  printf '%s\n' "\\set pw '${SENHA}'"
  cat <<'SQL'
-- Se o CREATE/ALTER falhar, o Postgres registraria o comando inteiro (com a
-- senha) no log do container, que o Alloy manda para o Loki. Só nesta sessão.
SET log_min_error_statement = panic;
SET log_statement = 'none';
SELECT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'monitoring') AS existe \gset
\if :existe
  ALTER ROLE monitoring WITH LOGIN PASSWORD :'pw' CONNECTION LIMIT 3;
\else
  CREATE ROLE monitoring WITH LOGIN PASSWORD :'pw' CONNECTION LIMIT 3;
\endif
GRANT pg_monitor TO monitoring;
SQL
} | docker exec -i "$PG_CONTAINER" psql -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q

echo "✅ Papel monitoring pronto."

if docker ps -q -f 'name=^geosegbar-prometheus$' | grep -q .; then
  sleep 20
  if docker exec geosegbar-prometheus wget -qO- http://postgres-exporter:9187/metrics 2>/dev/null \
       | grep -q '^pg_up 1'; then
    echo "✅ postgres-exporter conectado (pg_up 1)."
  else
    echo "⚠️  postgres-exporter ainda sem conexão. Veja: docker logs --tail 30 geosegbar-postgres-exporter"
  fi
fi
