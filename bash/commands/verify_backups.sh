#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/../lib/common.sh"

main() {
    ensure_cli_context
    ensure_root_dir

    local script="$ROOT_DIR/bash/scripts/verify_backups.sh"
    ensure_file_exists "$script"
    ensure_executable "$script"

    # Só leitura — não pede confirmação nem autenticação.
    bash "$script"
}

main "$@"
