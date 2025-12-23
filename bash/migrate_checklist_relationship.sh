#!/bin/bash

set -e

echo "🔄 Script de Migração: Checklist Dam Relationship"
echo "=================================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para exibir mensagens coloridas
info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
}

# Verificar argumentos
if [ "$#" -ne 1 ]; then
    error "Uso: $0 [dev|prod]"
    echo ""
    echo "Exemplos:"
    echo "  $0 dev   # Migrar ambiente de desenvolvimento"
    echo "  $0 prod  # Migrar ambiente de produção"
    exit 1
fi

ENVIRONMENT=$1

# Validar ambiente
if [ "$ENVIRONMENT" != "dev" ] && [ "$ENVIRONMENT" != "prod" ]; then
    error "Ambiente inválido: $ENVIRONMENT"
    echo "Use 'dev' ou 'prod'"
    exit 1
fi

# Diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

# Arquivo de migração SQL
MIGRATION_FILE="$SCRIPT_DIR/src/main/resources/db/migration/V1__migrate_checklist_dam_relationship.sql"

if [ ! -f "$MIGRATION_FILE" ]; then
    error "Arquivo de migração não encontrado: $MIGRATION_FILE"
    exit 1
fi

# Carregar variáveis de ambiente apropriadas
if [ "$ENVIRONMENT" = "dev" ]; then
    ENV_FILE=".env"
    CONTAINER_NAME="geosegbar-postgres-dev"
else
    ENV_FILE=".env.prod"
    CONTAINER_NAME="postgres-prod"
fi

if [ ! -f "$ENV_FILE" ]; then
    error "Arquivo de ambiente não encontrado: $ENV_FILE"
    exit 1
fi

# Carregar variáveis
set -a
source "$ENV_FILE"
set +a

info "Ambiente: $(echo $ENVIRONMENT | tr '[:lower:]' '[:upper:]')"
info "Container: $CONTAINER_NAME"
info "Database: $DB_NAME"
echo ""

# Verificar se o container está rodando
if ! docker ps -q -f name="$CONTAINER_NAME" | grep -q .; then
    error "Container $CONTAINER_NAME não está rodando!"
    echo ""
    echo "Para iniciar:"
    if [ "$ENVIRONMENT" = "dev" ]; then
        echo "  ./bash/dev.sh"
    else
        echo "  ./bash/deploy_vps.sh"
    fi
    exit 1
fi

success "Container do banco de dados está rodando"

# Criar backup antes da migração
BACKUP_FILE="backup_checklist_dam_$(date +%Y%m%d_%H%M%S).sql"
info "Criando backup das tabelas afetadas..."

docker exec "$CONTAINER_NAME" pg_dump \
    -U "$DB_USERNAME" \
    -d "$DB_NAME" \
    -t checklists \
    -t checklist_dam \
    --inserts \
    > "$BACKUP_FILE" 2>/dev/null

if [ $? -eq 0 ]; then
    success "Backup criado: $BACKUP_FILE"
else
    error "Falha ao criar backup!"
    exit 1
fi

# Verificar estado atual antes da migração
info "Verificando estado atual do banco..."
echo ""

CHECKLIST_COUNT=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USERNAME" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM checklists;" 2>/dev/null | xargs)

CHECKLIST_DAM_COUNT=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USERNAME" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM checklist_dam;" 2>/dev/null | xargs)

echo "📊 Estado atual:"
echo "   - Checklists: $CHECKLIST_COUNT"
echo "   - Relações checklist_dam: $CHECKLIST_DAM_COUNT"
echo ""

if [ "$CHECKLIST_DAM_COUNT" -eq 0 ]; then
    warning "Tabela checklist_dam está vazia. Migração pode não ser necessária."
fi

# Perguntar confirmação
echo ""
warning "⚠️  ATENÇÃO: Esta migração irá:"
echo "   1. Adicionar coluna dam_id na tabela checklists"
echo "   2. Migrar dados da tabela checklist_dam para checklists.dam_id"
echo "   3. Criar foreign key constraint"
echo "   4. Remover relacionamento ManyToMany (tabela checklist_dam ficará para remoção manual)"
echo ""
echo "   Um backup foi criado em: $BACKUP_FILE"
echo ""

read -p "Deseja continuar com a migração? (sim/não): " CONFIRM

if [ "$CONFIRM" != "sim" ] && [ "$CONFIRM" != "s" ] && [ "$CONFIRM" != "yes" ] && [ "$CONFIRM" != "y" ]; then
    info "Migração cancelada pelo usuário"
    exit 0
fi

echo ""
info "Executando migração..."
echo ""

# Executar migração
docker exec -i "$CONTAINER_NAME" psql -U "$DB_USERNAME" -d "$DB_NAME" < "$MIGRATION_FILE"

if [ $? -ne 0 ]; then
    error "Falha ao executar migração!"
    echo ""
    warning "Para restaurar o backup:"
    echo "  docker exec -i $CONTAINER_NAME psql -U $DB_USERNAME -d $DB_NAME < $BACKUP_FILE"
    exit 1
fi

echo ""
success "Migração executada com sucesso!"
echo ""

# Verificar resultados da migração
info "Verificando resultados..."
echo ""

# Verificar se todos os checklists têm dam_id
ORPHAN_COUNT=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USERNAME" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM checklists WHERE dam_id IS NULL;" 2>/dev/null | xargs)

if [ "$ORPHAN_COUNT" -gt 0 ]; then
    warning "Foram encontrados $ORPHAN_COUNT checklist(s) órfão(s) sem dam_id"
    echo ""
    echo "Execute para ver detalhes:"
    echo "  docker exec $CONTAINER_NAME psql -U $DB_USERNAME -d $DB_NAME -c \"SELECT id, name FROM checklists WHERE dam_id IS NULL;\""
else
    success "Todos os checklists possuem dam_id"
fi

# Verificar dams com múltiplos checklists
DUPLICATES=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USERNAME" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM (SELECT dam_id FROM checklists GROUP BY dam_id HAVING COUNT(*) > 1) AS duplicates;" 2>/dev/null | xargs)

if [ "$DUPLICATES" -gt 0 ]; then
    warning "Foram encontradas $DUPLICATES dam(s) com múltiplos checklists"
    echo ""
    echo "Execute para ver detalhes:"
    echo "  docker exec $CONTAINER_NAME psql -U $DB_USERNAME -d $DB_NAME -c \"SELECT dam_id, COUNT(*) as total FROM checklists GROUP BY dam_id HAVING COUNT(*) > 1;\""
else
    success "Cada dam possui no máximo 1 checklist"
fi

echo ""
echo "📊 Estado após migração:"

CHECKLIST_WITH_DAM=$(docker exec "$CONTAINER_NAME" psql -U "$DB_USERNAME" -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM checklists WHERE dam_id IS NOT NULL;" 2>/dev/null | xargs)

echo "   - Checklists com dam_id: $CHECKLIST_WITH_DAM"
echo "   - Checklists sem dam_id: $ORPHAN_COUNT"
echo ""

# Instruções finais
echo "=================================================="
success "Migração concluída!"
echo ""
info "Próximos passos:"
echo ""
echo "1. Verifique se a aplicação está funcionando corretamente"
if [ "$ENVIRONMENT" = "dev" ]; then
    echo "   docker-compose logs -f geosegbar-api"
else
    echo "   docker logs -f geosegbar-api-prod"
fi
echo ""
echo "2. Se tudo estiver OK, remova a tabela checklist_dam:"
echo "   docker exec -it $CONTAINER_NAME psql -U $DB_USERNAME -d $DB_NAME -c \"DROP TABLE IF EXISTS checklist_dam;\""
echo ""
echo "3. O backup está salvo em: $BACKUP_FILE"
echo "   (Mantenha este arquivo por segurança)"
echo ""
warning "⚠️  Reinicie a aplicação para que as mudanças tenham efeito completo"
if [ "$ENVIRONMENT" = "dev" ]; then
    echo "   docker-compose restart geosegbar-api"
else
    echo "   docker restart geosegbar-api-prod"
fi
echo ""
