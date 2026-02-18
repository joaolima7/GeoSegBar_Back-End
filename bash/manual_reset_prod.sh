#!/bin/bash

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "\n${RED}⚠️  ATENÇÃO: RESET TOTAL DE PRODUÇÃO (MODO MANUAL)${NC}"
echo -e "${RED}Isso irá APAGAR PERMANENTEMENTE o banco de dados e todos os dados!${NC}"
echo -e "${YELLOW}Pressione ENTER para continuar ou CTRL+C para cancelar...${NC}"
read

echo -e "${BLUE}🛑 Parando e removendo containers...${NC}"

# Lista de containers para remover
CONTAINERS=("nginx-prod" "geosegbar-api-prod" "postgres-prod" "postgres-exporter-prod" "redis-prod" "prometheus-prod" "grafana-prod")

for container in "${CONTAINERS[@]}"; do
    if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "   Removendo $container..."
        docker stop $container >/dev/null 2>&1
        docker rm -f $container >/dev/null 2>&1
    else
        echo "   $container não encontrado (ok)."
    fi
done

echo -e "${RED}🗑️  Removendo volumes persistentes...${NC}"

# Lista de volumes para remover
VOLUMES=("postgres-prod-data" "redis-prod-data" "prometheus-prod-data" "grafana-prod-data")

for volume in "${VOLUMES[@]}"; do
    if docker volume ls -q | grep -q "^${volume}$"; then
        echo "   Removendo volume $volume..."
        docker volume rm $volume
    else
        echo "   Volume $volume não existe (ok)."
    fi
done

echo -e "${BLUE}🧹 Limpando redes...${NC}"
docker network rm geosegbar-network >/dev/null 2>&1 || true

echo -e "${BLUE}🧹 Limpando imagens antigas...${NC}"
docker image prune -a -f

# Limpeza opcional de arquivos físicos
# Carregar variável FILE_UPLOAD_DIR do .env.prod se existir
if [ -f .env.prod ]; then
    source .env.prod
    if [ -n "$FILE_UPLOAD_DIR" ]; then
        echo ""
        echo -e "${YELLOW}Deseja limpar os arquivos físicos de upload em: $FILE_UPLOAD_DIR ? (s/n)${NC}"
        read -r REPLY
        if [[ $REPLY =~ ^[Ss]$ ]]; then
            echo "   Limpando uploads..."
            rm -rf "$FILE_UPLOAD_DIR"/* 2>/dev/null
        fi
    fi
fi

echo ""
echo -e "${GREEN}✅ Limpeza completa! O ambiente está zerado.${NC}"
echo -e "${BLUE}🚀 Agora você pode rodar: ./bash/deploy_vps.sh${NC}"