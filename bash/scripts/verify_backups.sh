#!/usr/bin/env bash
#
# Mostra o estado real dos backups: o que existe em disco, o que existe no S3, e
# se o cron está de fato apontando para os scripts certos.
#
# Só leitura. Serve para responder "meus backups estão acontecendo?" sem depender
# de acreditar que estão.

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

BACKUP_BASE_DIR="${BACKUP_BASE_DIR:-$HOME/db_backups}"
STATUS_FILE="$SCRIPT_DIR/logs/backup_status.json"
S3_PREFIX="${BACKUP_S3_PREFIX:-db-backups}"

if [[ -f "$SCRIPT_DIR/.env.prod" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$SCRIPT_DIR/.env.prod"
  set +a
fi

echo "════════════════════════════════════════════════════════"
echo " ESTADO DOS BACKUPS"
echo "════════════════════════════════════════════════════════"
echo ""

# ------------------------------------------------------------------ cron
echo "📅 TAREFAS AGENDADAS"
ENTRADAS="$(crontab -l 2>/dev/null | grep -E "backup_database_prod\.sh|check_backup_freshness\.sh" || true)"
if [[ -z "$ENTRADAS" ]]; then
  echo "   ❌ Nenhum cron de backup instalado"
  echo "      Configure em: ./bash/cli_app.sh -> Configurar cron de backup"
else
  while IFS= read -r linha; do
    caminho="$(printf '%s' "$linha" | grep -oE '/[^ ]*\.sh' | head -1)"
    horario="$(printf '%s' "$linha" | awk '{print $1, $2, $3, $4, $5}')"
    if [[ -f "$caminho" ]]; then
      echo "   ✅ [$horario] $(basename "$caminho")"
    else
      echo "   ❌ [$horario] $caminho — CAMINHO NÃO EXISTE (cron quebrado)"
    fi
  done <<< "$ENTRADAS"
fi
echo ""

# ------------------------------------------------------------- último status
echo "🕐 ÚLTIMA EXECUÇÃO"
if [[ -f "$STATUS_FILE" ]]; then
  EPOCH="$(grep -oE '"epoch"[[:space:]]*:[[:space:]]*[0-9]+' "$STATUS_FILE" | grep -oE '[0-9]+$' || echo 0)"
  RESULTADO="$(grep -oE '"resultado"[[:space:]]*:[[:space:]]*"[^"]*"' "$STATUS_FILE" | sed 's/.*"\([^"]*\)"$/\1/' || echo '?')"
  if [[ "$EPOCH" -gt 0 ]]; then
    IDADE_H=$(( ($(date +%s) - EPOCH) / 3600 ))
    case "$RESULTADO" in
      sucesso) icone="✅" ;;
      parcial) icone="⚠️ " ;;
      *)       icone="❌" ;;
    esac
    echo "   ${icone} ${RESULTADO} — há ${IDADE_H}h ($(date -d "@$EPOCH" '+%d/%m/%Y %H:%M' 2>/dev/null || date -r "$EPOCH" '+%d/%m/%Y %H:%M'))"
    [[ "$IDADE_H" -gt 48 ]] && echo "   ⚠️  Mais de 48h sem backup — algo está errado"
  else
    echo "   ❌ Arquivo de status ilegível"
  fi
else
  echo "   ❌ Backup nunca executou (sem $STATUS_FILE)"
fi
echo ""

# ------------------------------------------------------------------- local
echo "💾 CÓPIAS LOCAIS  ($BACKUP_BASE_DIR)"
if [[ -d "$BACKUP_BASE_DIR" ]]; then
  QTD="$(find "$BACKUP_BASE_DIR" -type f -name '*.sql.gz' | wc -l)"
  if [[ "$QTD" -eq 0 ]]; then
    echo "   ❌ Nenhum arquivo de backup"
  else
    echo "   $QTD arquivo(s), $(du -sh "$BACKUP_BASE_DIR" 2>/dev/null | cut -f1) no total"
    find "$BACKUP_BASE_DIR" -type f -name '*.sql.gz' -printf '%T@ %TY-%Tm-%Td %TH:%TM  %8s  %p\n' 2>/dev/null \
      | sort -rn | head -5 \
      | while read -r _ data hora tam caminho; do
          printf "   • %s %s  %6s KB  %s\n" "$data" "$hora" "$((tam / 1024))" "$(basename "$caminho")"
        done
  fi
else
  echo "   ❌ Diretório não existe"
fi
echo ""

# ---------------------------------------------------------------------- S3
echo "☁️  CÓPIAS NO S3  (s3://${AWS_BUCKET_NAME:-?}/${S3_PREFIX}/)"
if [[ -z "${AWS_BUCKET_NAME:-}" || -z "${AWS_ACCESS_KEY_ID:-}" ]]; then
  echo "   ⚠️  Credenciais AWS ausentes no .env.prod"
else
  # Listagem pelo assinador próprio: o --aws-sigv4 do curl 7.76 erra a
  # assinatura quando há query string, e listar exige ?list-type=2&prefix=...
  LISTAGEM="$(python3 "$SCRIPT_DIR/bash/scripts/lib/s3_client.py" \
                list "$AWS_BUCKET_NAME" "$AWS_REGION" "${S3_PREFIX}/" 2>&1)"

  if [[ $? -ne 0 ]]; then
    echo "   ❌ Não foi possível listar o bucket: ${LISTAGEM}"
  elif [[ -z "$LISTAGEM" ]]; then
    echo "   ❌ Nenhum backup no S3 ainda"
  else
    echo "   $(printf '%s\n' "$LISTAGEM" | wc -l | tr -d ' ') arquivo(s). Mais recentes:"
    printf '%s\n' "$LISTAGEM" | sort -t$'\t' -k2 -r | head -5 \
      | while IFS=$'\t' read -r tam data chave; do
          printf "   • %s  %6s KB  %s\n" "${data:0:16}" "$((tam / 1024))" "$(basename "$chave")"
        done
  fi
fi

echo ""
echo "════════════════════════════════════════════════════════"
