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

    local rollback_script="$ROOT_DIR/bash/scripts/rollback_prod.sh"
    ensure_file_exists "$rollback_script"
    ensure_executable "$rollback_script"

    warn "Rollback devolve a API para a imagem imediatamente anterior."
    warn "O BANCO NÃO é revertido — migrações já aplicadas continuam valendo."

    if ! confirm "Deseja prosseguir para o rollback de produção?"; then
        warn "Rollback cancelado pelo usuário."
        return 0
    fi

    info "Iniciando rollback de produção..."
    run_cmd "bash '$rollback_script'"
    info "Rollback finalizado."
}

main "$@"
