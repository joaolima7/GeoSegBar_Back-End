#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/../lib/common.sh"

main() {
    ensure_cli_context
    acquire_lock
    ensure_root_dir
    ensure_docker_up
    load_prod_env
    authenticate_cli_action

    local backup_script="$ROOT_DIR/bash/scripts/backup_database_prod.sh"

    if [[ ! -f "$backup_script" ]]; then
        die "Script de backup não encontrado: $backup_script"
    fi

    ensure_executable "$backup_script"

    if ! confirm "Deseja executar o backup do banco de produção agora?"; then
        warn "Backup cancelado pelo usuário."
        return 0
    fi

    info "Executando backup de banco..."
    run_cmd "bash '$backup_script'"
    info "Backup concluído."
}

main "$@"
