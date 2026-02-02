#!/bin/bash

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f .env.prod ]; then
    echo -e "${RED}❌ Arquivo .env.prod não encontrado!${NC}"
    exit 1
fi

set -a
source .env.prod
set +a

echo -e "${BLUE}🛑 Parando todos os serviços com Docker Compose...${NC}"
docker-compose -f docker-compose.prod.yml down

echo ""
echo -e "${GREEN}✅ Todos os serviços foram parados com segurança${NC}"
echo ""
echo -e "${BLUE}📊 Status dos containers:${NC}"
docker-compose -f docker-compose.prod.yml ps

echo ""
echo -e "${YELLOW}💡 Para reiniciar os serviços, execute:${NC}"
echo "   ./bash/deploy_prod_compose.sh"
echo ""
