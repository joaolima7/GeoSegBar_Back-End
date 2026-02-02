#!/bin/bash

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${RED}⚠️  ATENÇÃO: Este script irá APAGAR TUDO em produção!${NC}"
echo -e "${RED}Incluindo banco de dados, volumes e containers${NC}"
echo ""
echo -e "${YELLOW}Pressione CTRL+C para cancelar ou aguarde 10 segundos...${NC}"
sleep 10

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

# Carregar variáveis
if [ ! -f .env.prod ]; then
    echo -e "${RED}❌ Arquivo .env.prod não encontrado!${NC}"
    exit 1
fi

set -a
source .env.prod
set +a

echo -e "${BLUE}🛑 Parando todos os serviços...${NC}"
docker-compose -f docker-compose.prod.yml down 2>/dev/null || true

echo -e "${RED}🗑️  Removendo volumes de dados...${NC}"
# Remove volumes específicos
docker volume rm geosegbar_postgres-prod-data 2>/dev/null || echo "   Volume postgres-prod-data não encontrado"
docker volume rm geosegbar_prometheus-prod-data 2>/dev/null || echo "   Volume prometheus-prod-data não encontrado"
docker volume rm geosegbar_grafana-prod-data 2>/dev/null || echo "   Volume grafana-prod-data não encontrado"
docker volume rm geosegbar_redis-prod-data 2>/dev/null || echo "   Volume redis-prod-data não encontrado"

# Remove volumes órfãos
echo -e "${BLUE}🧹 Removendo volumes órfãos...${NC}"
docker volume prune -f 2>/dev/null || true

echo -e "${BLUE}🗑️  Limpando diretórios de dados locais...${NC}"
rm -rf ./prometheus-prod/* 2>/dev/null || true
rm -rf ./grafana-prod/dashboards/* 2>/dev/null || true
rm -rf ./logs/* 2>/dev/null || true

# Opção de limpar storage
echo ""
echo -e "${YELLOW}Deseja limpar também o diretório de upload de arquivos?${NC}"
echo "Este diretório contém: ${FILE_UPLOAD_DIR}"
read -p "Digite 's' para sim ou 'n' para não: " -r
if [[ $REPLY =~ ^[Ss]$ ]]; then
    echo -e "${RED}🗑️  Limpando diretório de upload...${NC}"
    rm -rf ${FILE_UPLOAD_DIR}/* 2>/dev/null || echo "   Erro ao limpar (talvez necessite sudo)"
    echo -e "${GREEN}✅ Diretório de upload limpo${NC}"
fi

echo ""
echo -e "${GREEN}✅ Limpeza completa realizada!${NC}"
echo -e "${BLUE}🚀 Agora execute: ./bash/deploy_prod_compose.sh${NC}"
echo ""
