#!/usr/bin/env bash

set -Eeuo pipefail

if [[ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]]; then
	echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
	exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

BACKUP_SCRIPT="$SCRIPT_DIR/bash/scripts/backup_database_prod.sh"
CRON_SCHEDULE="${BACKUP_CRON_SCHEDULE:-0 2 * * *}"
CRON_LOG="$SCRIPT_DIR/logs/cron_backup.log"

[[ -f "$BACKUP_SCRIPT" ]] || { echo "Script de backup não encontrado: $BACKUP_SCRIPT"; exit 1; }
chmod +x "$BACKUP_SCRIPT"
mkdir -p "$SCRIPT_DIR/logs"

CRON_JOB="$CRON_SCHEDULE bash $BACKUP_SCRIPT >> $CRON_LOG 2>&1"

CURRENT_CRONTAB="$(crontab -l 2>/dev/null || true)"
FILTERED_CRONTAB="$(echo "$CURRENT_CRONTAB" | grep -v "$BACKUP_SCRIPT" || true)"

printf "%s\n%s\n" "$FILTERED_CRONTAB" "$CRON_JOB" | sed '/^$/N;/^\n$/D' | crontab -

echo "Cron de backup configurado com sucesso"
echo "Schedule: $CRON_SCHEDULE"
echo "Job: $CRON_JOB"
