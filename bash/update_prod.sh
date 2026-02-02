#!/bin/bash

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔄 Update da Aplicação em PRODUÇÃO${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f .env.prod ]; then
    echo -e "${RED}❌ Arquivo .env.prod não encontrado!${NC}"
    exit 1
fi

set -a
source .env.prod
set +a

echo -e "${BLUE}📥 Atualizando código do repositório...${NC}"
git pull origin main

echo -e "${BLUE}🔨 Construindo nova imagem Docker...${NC}"
docker-compose -f docker-compose.prod.yml build geosegbar-api

echo -e "${BLUE}🚀 Atualizando container da API...${NC}"
docker-compose -f docker-compose.prod.yml up -d geosegbar-api

echo -e "${BLUE}⏳ Aguardando API reinicializar...${NC}"
sleep 15

TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if curl -sf http://localhost:${SERVER_PORT}/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ API está HEALTHY!${NC}"
        break
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo "   Tentativa $((ELAPSED/5))... (${ELAPSED}s/${TIMEOUT}s)"
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    echo -e "${RED}❌ API não ficou healthy${NC}"
    echo -e "${YELLOW}📋 Verificando logs:${NC}"
    docker-compose -f docker-compose.prod.yml logs --tail 50 geosegbar-api
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 Update realizado com sucesso!${NC}"
echo ""
echo -e "${BLUE}📊 Status dos containers:${NC}"
docker-compose -f docker-compose.prod.yml ps
echo ""
