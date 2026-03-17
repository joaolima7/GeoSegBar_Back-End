#!/usr/bin/env bash

set -Eeuo pipefail

if [[ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

[[ -f .env.prod ]] || { echo "❌ .env.prod não encontrado"; exit 1; }

set -a
# shellcheck disable=SC1091
source .env.prod
set +a

BACKUP_BASE_DIR="${BACKUP_BASE_DIR:-$HOME/db_backups}"
BACKUP_SCRIPT="$SCRIPT_DIR/bash/scripts/backup_database_prod.sh"
DEPLOY_SCRIPT="$SCRIPT_DIR/bash/scripts/deploy_vps.sh"

mkdir -p "$BACKUP_BASE_DIR"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

confirm_yes_no() {
  local prompt="$1"
  read -r -p "$prompt [y/N]: " answer
  [[ "$answer" =~ ^[Yy]$ ]]
}

find_latest_backup() {
  find "$BACKUP_BASE_DIR" -type f -name "*.sql.gz" | sort -r | head -n 1
}

has_backup_older_than_1_day() {
  find "$BACKUP_BASE_DIR" -type f -name "*.sql.gz" -mtime +0 | grep -q .
}

ensure_safety_backup() {
  if has_backup_older_than_1_day; then
    log "✅ Existe backup com pelo menos 1 dia de idade."
    return 0
  fi

  log "⚠️ Não existe backup com pelo menos 1 dia. Criando backup de segurança agora..."
  bash "$BACKUP_SCRIPT"
  log "✅ Backup de segurança criado."
}

cleanup_docker_environment() {
  log "🧹 Limpando ambiente Docker completo (containers, imagens, volumes, networks órfãs)..."
  docker ps -aq | xargs -r docker rm -f
  docker system prune -a --volumes -f
  log "✅ Limpeza Docker concluída."
}

prepare_db_with_deploy_script() {
  log "🚀 Preparando PostgreSQL/Redis via deploy_vps.sh (modo DB_ONLY)..."
  DEPLOY_MODE=DB_ONLY SKIP_GIT_PULL=true GEOSEGBAR_CLI_CONTEXT=1 bash "$DEPLOY_SCRIPT"
  log "✅ Base PostgreSQL/Redis preparada com configuração padrão de deploy."
}

restore_database_from_backup() {
  local backup_file="$1"

  [[ -f "$backup_file" ]] || { log "❌ Backup não encontrado: $backup_file"; exit 1; }

  log "📥 Restaurando banco a partir de: $backup_file"

  docker exec postgres-prod psql -U "$DB_USERNAME" -d postgres -c "DROP DATABASE IF EXISTS \"$DB_NAME\";" >/dev/null
  docker exec postgres-prod psql -U "$DB_USERNAME" -d postgres -c "CREATE DATABASE \"$DB_NAME\";" >/dev/null

  gunzip -c "$backup_file" | docker exec -i postgres-prod psql -U "$DB_USERNAME" -d "$DB_NAME" >/dev/null

  log "✅ Restore do banco concluído."
}

main() {
  docker info >/dev/null 2>&1 || { echo "❌ Docker não está rodando"; exit 1; }

  ensure_safety_backup

  local apply_redeploy=false
  local apply_restore=false

  if confirm_yes_no "Deseja refazer o deploy após a limpeza?"; then
    apply_redeploy=true
    if confirm_yes_no "Deseja aplicar backup do banco no redeploy?"; then
      apply_restore=true
    fi
  fi

  local latest_backup
  latest_backup="$(find_latest_backup || true)"

  cleanup_docker_environment

  if [[ "$apply_redeploy" == "true" ]]; then
    if [[ "$apply_restore" == "true" ]]; then
      if [[ -z "$latest_backup" ]]; then
        log "❌ Nenhum backup encontrado para restore. Cancelando restore e seguindo sem restore."
      else
        prepare_db_with_deploy_script
        restore_database_from_backup "$latest_backup"
      fi
    fi

    log "🚀 Executando redeploy completo..."
    bash "$DEPLOY_SCRIPT"
  else
    log "ℹ️ Limpeza concluída sem redeploy."
  fi

  log "✅ Reset de ambiente finalizado."
}

main "$@"
