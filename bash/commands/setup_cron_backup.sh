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

    local cron_script="$ROOT_DIR/bash/scripts/setup_cron_backup.sh"

    if [[ ! -f "$cron_script" ]]; then
        die "Script de configuração de cron não encontrado: $cron_script"
    fi

    ensure_executable "$cron_script"

    if ! confirm "Deseja configurar/atualizar o cron de backup agora?"; then
        warn "Configuração de cron cancelada pelo usuário."
        return 0
    fi

    info "Configurando rotina de cron para backup..."
    run_cmd "bash '$cron_script'"
    info "Cron de backup configurado."
}

main "$@"
