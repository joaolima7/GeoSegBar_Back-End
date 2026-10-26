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

    local script="$ROOT_DIR/bash/scripts/deploy_monitoring.sh"
    ensure_file_exists "$script"
    ensure_executable "$script"

    info "Deploy do monitoramento: valida monitoring/, instala em /opt/geosegbar-monitoring"
    info "e recria os containers da stack. A API não é reiniciada."
    if ! confirm "Deseja executar o deploy do monitoramento agora?"; then
        warn "Deploy do monitoramento cancelado pelo usuário."
        return 0
    fi

    # Sem run_cmd: a saída precisa aparecer na tela (o script pede segredos na
    # primeira execução e mostra o resultado de cada validação).
    bash "$script" 2>&1 | tee -a "$RUN_LOG_FILE"
}

main "$@"
