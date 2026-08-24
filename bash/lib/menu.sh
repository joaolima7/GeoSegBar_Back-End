#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/common.sh"

show_menu() {
    echo
    echo "================ GeoSegBar CLI ================"
    echo "1) Deploy produção (blue-green, sem downtime)"
    echo "2) Rollback produção (volta para a versão anterior)"
    echo "3) Backup banco produção (local + S3)"
    echo "4) Verificar estado dos backups"
    echo "5) Configurar cron de backup"
    echo "6) Reset completo de ambiente"
    echo "7) Sair"
    echo "==============================================="
}

menu_loop() {
    while true; do
        show_menu
        read -r -p "Escolha uma opção: " opt

        case "$opt" in
            1) bash "$ROOT_DIR/bash/commands/deploy_prod.sh" ;;
            2) bash "$ROOT_DIR/bash/commands/rollback_prod.sh" ;;
            3) bash "$ROOT_DIR/bash/commands/db_backup.sh" ;;
            4) bash "$ROOT_DIR/bash/commands/verify_backups.sh" ;;
            5) bash "$ROOT_DIR/bash/commands/setup_cron_backup.sh" ;;
            6) bash "$ROOT_DIR/bash/commands/reset_environment.sh" ;;
            7)
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
