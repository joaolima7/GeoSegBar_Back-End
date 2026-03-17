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

    local deploy_script="$ROOT_DIR/bash/scripts/deploy_vps.sh"
    ensure_file_exists "$deploy_script"
    ensure_executable "$deploy_script"

    if ! confirm "Deseja executar o deploy de produção agora?"; then
        warn "Deploy cancelado pelo usuário."
        return 0
    fi

    info "Iniciando deploy de produção via script legado..."
    run_cmd "bash '$deploy_script'"
    info "Deploy finalizado."
}

main "$@"
