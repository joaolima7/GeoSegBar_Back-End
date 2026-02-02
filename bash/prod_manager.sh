#!/bin/bash

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

# Função para exibir menu
show_menu() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║     GeoSegBar Production Deployment Manager           ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}Selecione uma opção:${NC}"
    echo ""
    echo -e "  ${GREEN}1${NC}) Primeiro Deploy - Começar do Zero (⚠️  Apaga TUDO)"
    echo -e "  ${GREEN}2${NC}) Deploy Normal - Atualizar Aplicação"
    echo -e "  ${GREEN}3${NC}) Update - Pull do Git + Rebuild"
    echo -e "  ${GREEN}4${NC}) Parar Serviços (mantém dados)"
    echo -e "  ${GREEN}5${NC}) Ver Status dos Containers"
    echo -e "  ${GREEN}6${NC}) Ver Logs em Tempo Real"
    echo -e "  ${GREEN}7${NC}) Restart de um Serviço"
    echo -e "  ${GREEN}8${NC}) Apagar TUDO (limpeza completa)"
    echo -e "  ${GREEN}0${NC}) Sair"
    echo ""
}

verify_env() {
    if [ ! -f .env.prod ]; then
        echo -e "${RED}❌ Arquivo .env.prod não encontrado!${NC}"
        echo "Crie o arquivo com: cp .env.example .env.prod"
        exit 1
    fi
}

first_deploy() {
    echo -e "${RED}⚠️  AVISO: Este processo irá apagar TUDO!${NC}"
    read -p "Tem certeza? (s/n): " -r
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        ./bash/clean_prod_complete.sh
        echo ""
        echo -e "${BLUE}Iniciando deploy...${NC}"
        sleep 3
        ./bash/deploy_prod_compose.sh
    fi
}

normal_deploy() {
    ./bash/deploy_prod_compose.sh
}

update_deploy() {
    ./bash/update_prod.sh
}

stop_services() {
    ./bash/stop_prod.sh
}

show_status() {
    if [ ! -f .env.prod ]; then return; fi
    set -a
    source .env.prod
    set +a
    
    echo -e "${BLUE}📊 Status dos Containers:${NC}"
    docker-compose -f docker-compose.prod.yml ps
}

show_logs() {
    echo -e "${BLUE}Selecione qual log deseja ver:${NC}"
    echo "  1) API"
    echo "  2) PostgreSQL"
    echo "  3) Prometheus"
    echo "  4) Grafana"
    echo "  5) Redis"
    echo "  0) Todos os serviços"
    read -p "Opção: " -r log_option
    
    case $log_option in
        1) docker-compose -f docker-compose.prod.yml logs -f geosegbar-api ;;
        2) docker-compose -f docker-compose.prod.yml logs -f postgres-prod ;;
        3) docker-compose -f docker-compose.prod.yml logs -f prometheus ;;
        4) docker-compose -f docker-compose.prod.yml logs -f grafana ;;
        5) docker-compose -f docker-compose.prod.yml logs -f redis-prod ;;
        0) docker-compose -f docker-compose.prod.yml logs -f ;;
        *) echo "Opção inválida" ;;
    esac
}

restart_service() {
    echo -e "${BLUE}Selecione qual serviço deseja reiniciar:${NC}"
    echo "  1) API (geosegbar-api)"
    echo "  2) PostgreSQL"
    echo "  3) Redis"
    echo "  4) Prometheus"
    echo "  5) Grafana"
    echo "  0) Todos"
    read -p "Opção: " -r service_option
    
    case $service_option in
        1) docker-compose -f docker-compose.prod.yml restart geosegbar-api ;;
        2) docker-compose -f docker-compose.prod.yml restart postgres-prod ;;
        3) docker-compose -f docker-compose.prod.yml restart redis-prod ;;
        4) docker-compose -f docker-compose.prod.yml restart prometheus ;;
        5) docker-compose -f docker-compose.prod.yml restart grafana ;;
        0) docker-compose -f docker-compose.prod.yml restart ;;
        *) echo "Opção inválida" ;;
    esac
}

clean_everything() {
    echo -e "${RED}⚠️  AVISO FINAL: Esta ação é irreversível!${NC}"
    echo -e "${RED}Você irá perder TODOS os dados do banco e volumes!${NC}"
    echo ""
    read -p "Digite 'SIM' (em maiúsculas) para continuar: " -r confirm
    if [[ $confirm == "SIM" ]]; then
        ./bash/clean_prod_complete.sh
    else
        echo "Operação cancelada."
    fi
}

# Loop principal
verify_env

while true; do
    show_menu
    read -p "Digite sua escolha (0-8): " -r choice
    
    case $choice in
        1) first_deploy ;;
        2) normal_deploy ;;
        3) update_deploy ;;
        4) stop_services ;;
        5) show_status ;;
        6) show_logs ;;
        7) restart_service ;;
        8) clean_everything ;;
        0) 
            echo -e "${GREEN}Até logo!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Opção inválida!${NC}"
            sleep 2
            ;;
    esac
done
