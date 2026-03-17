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

    local reset_script="$ROOT_DIR/bash/scripts/reset_environment.sh"
    ensure_file_exists "$reset_script"
    ensure_executable "$reset_script"

    if ! confirm "ATENÇÃO: Reset vai limpar containers/imagens/volumes. Deseja continuar?"; then
        warn "Reset cancelado pelo usuário."
        return 0
    fi

    info "Iniciando reset completo de ambiente..."
    run_cmd "bash '$reset_script'"
    info "Reset de ambiente concluído."
}

main "$@"
