#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/lib/common.sh"

usage() {
    cat <<'EOF'
GeoSegBar CLI

Uso:
    ./bash/cli_app.sh
  ./bash/cli_app.sh help

Ações disponíveis no menu:
    - Deploy produção
    - Backup banco produção
    - Configurar cron de backup
    - Reset completo de ambiente
EOF
}

parse_global_flags() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --yes|-y)
                ASSUME_YES=true
                shift
                ;;
            --verbose|-v)
                VERBOSE=true
                shift
                ;;
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            *)
                break
                ;;
        esac
    done

    REMAINING_ARGS=("$@")
}

main() {
    ensure_root_dir

    local cmd="${1:-menu}"
    shift || true

    case "$cmd" in
        help|-h|--help)
            usage
            return 0
            ;;
        menu)
            ;;
        *)
            warn "Modo seguro ativo: subcomandos diretos desabilitados. Abrindo menu interativo..."
            ;;
    esac

    parse_global_flags "$@"
    export ASSUME_YES VERBOSE DRY_RUN GEOSEGBAR_CLI_CONTEXT=1
    bash "$SCRIPT_DIR/lib/menu.sh"
}

main "$@"
