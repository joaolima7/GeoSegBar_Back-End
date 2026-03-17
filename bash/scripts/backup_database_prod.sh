#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

LOG_FILE="$SCRIPT_DIR/logs/backup.log"
BACKUP_BASE_DIR="${BACKUP_BASE_DIR:-$HOME/db_backups}"
BACKUP_KEEP_COUNT="${BACKUP_KEEP_COUNT:-7}"

mkdir -p "$SCRIPT_DIR/logs"
mkdir -p "$BACKUP_BASE_DIR"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

die() {
  log "ERROR: $1"
  exit 1
}

[[ -f "$SCRIPT_DIR/.env.prod" ]] || die "Arquivo .env.prod não encontrado"

set -a
# shellcheck disable=SC1091
source "$SCRIPT_DIR/.env.prod"
set +a

docker info >/dev/null 2>&1 || die "Docker não está rodando"
docker ps -q -f name=postgres-prod | grep -q . || die "Container postgres-prod não está rodando"

[[ "$BACKUP_KEEP_COUNT" =~ ^[0-9]+$ ]] || die "BACKUP_KEEP_COUNT deve ser numérico"
[[ "$BACKUP_KEEP_COUNT" -gt 0 ]] || die "BACKUP_KEEP_COUNT deve ser maior que zero"

BACKUP_DIR="$BACKUP_BASE_DIR/$(date +%Y)/$(date +%m)"
BACKUP_FILE="$BACKUP_DIR/geosegbar_backup_$(date +%Y%m%d_%H%M%S).sql"
GZIP_FILE="$BACKUP_FILE.gz"

mkdir -p "$BACKUP_DIR"

log "Iniciando backup do banco: $DB_NAME"
if docker exec postgres-prod pg_dump -U "$DB_USERNAME" "$DB_NAME" > "$BACKUP_FILE"; then
  gzip -f "$BACKUP_FILE"
  log "Backup concluído: $GZIP_FILE"
else
  die "Falha ao executar pg_dump"
fi

mapfile -t ALL_BACKUPS < <(find "$BACKUP_BASE_DIR" -type f -name "*.sql.gz" | sort -r)
TOTAL_BACKUPS="${#ALL_BACKUPS[@]}"
REMOVED_BACKUPS=0

if (( TOTAL_BACKUPS > BACKUP_KEEP_COUNT )); then
  for (( i=BACKUP_KEEP_COUNT; i<TOTAL_BACKUPS; i++ )); do
    rm -f "${ALL_BACKUPS[$i]}"
    REMOVED_BACKUPS=$((REMOVED_BACKUPS + 1))
  done
fi

CURRENT_TOTAL=$((TOTAL_BACKUPS - REMOVED_BACKUPS))
log "Rotação concluída (mantidos: ${BACKUP_KEEP_COUNT}, removidos: ${REMOVED_BACKUPS}, total atual: ${CURRENT_TOTAL})"
