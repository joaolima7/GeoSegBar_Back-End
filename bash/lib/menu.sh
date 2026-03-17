#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/common.sh"

show_menu() {
    echo
    echo "================ GeoSegBar CLI ================"
    echo "1) Deploy produção"
    echo "2) Backup banco produção"
    echo "3) Configurar cron de backup"
    echo "4) Reset completo de ambiente"
    echo "5) Sair"
    echo "==============================================="
}

menu_loop() {
    while true; do
        show_menu
        read -r -p "Escolha uma opção: " opt

        case "$opt" in
            1) bash "$ROOT_DIR/bash/commands/deploy_prod.sh" ;;
            2) bash "$ROOT_DIR/bash/commands/db_backup.sh" ;;
            3) bash "$ROOT_DIR/bash/commands/setup_cron_backup.sh" ;;
            4) bash "$ROOT_DIR/bash/commands/reset_environment.sh" ;;
            5)
                info "Encerrando CLI."
                exit 0
                ;;
            *)
                warn "Opção inválida: $opt"
                ;;
        esac
    done
}

menu_loop
