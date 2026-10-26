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

    local script="$ROOT_DIR/bash/scripts/monitoring/create_pg_monitoring_role.sh"
    ensure_file_exists "$script"
    ensure_executable "$script"

    # O próprio script mostra o SQL e pede confirmação digitada.
    bash "$script" 2>&1 | tee -a "$RUN_LOG_FILE"
}

main "$@"
