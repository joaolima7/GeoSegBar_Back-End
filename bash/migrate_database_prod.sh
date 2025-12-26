#!/bin/bash

set -e

echo "🔄 Executando migração do banco de dados em PRODUÇÃO..."

# Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

# Verificar se o arquivo .env.prod existe
if [ ! -f .env.prod ]; then
    echo "❌ Arquivo .env.prod não encontrado!"
    exit 1
fi

# Carregar variáveis do .env.prod
set -a
source .env.prod
set +a

echo "📦 Carregando variáveis de ambiente do arquivo .env.prod..."
echo "🔧 Profile ativo: ${SPRING_PROFILES_ACTIVE}"
echo "🗄️  Banco: ${DB_NAME}@${DB_HOST}:${DB_PORT}"

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Verificar se o container do PostgreSQL está rodando
if ! docker ps | grep -q "geosegbar-postgres-prod"; then
    echo "❌ Container do PostgreSQL (geosegbar-postgres-prod) não está rodando!"
    echo "💡 Execute primeiro: ./bash/deploy_vps.sh"
    exit 1
fi

echo ""
echo "⚠️  ⚠️  ⚠️  ATENÇÃO: AMBIENTE DE PRODUÇÃO ⚠️  ⚠️  ⚠️"
echo ""
echo "   Você está prestes a executar migrações no banco de PRODUÇÃO"
echo "   Banco: ${DB_NAME}"
echo "   Host: ${DB_HOST}"
echo ""
echo "   Esta operação pode afetar dados em produção!"
echo ""
read -p "Você TEM CERTEZA que deseja continuar? (digite 'CONFIRMO'): " -r
echo
if [[ ! $REPLY == "CONFIRMO" ]]; then
    echo "❌ Migração cancelada pelo usuário"
    exit 1
fi

echo ""
echo "💾 Criando backup do banco ANTES da migração..."
BACKUP_FILE="backup_pre_migration_$(date +%Y%m%d_%H%M%S).sql"
docker exec geosegbar-postgres-prod pg_dump -U ${DB_USERNAME} ${DB_NAME} > "./logs/${BACKUP_FILE}"

if [ $? -eq 0 ]; then
    echo "✅ Backup criado com sucesso: ./logs/${BACKUP_FILE}"
else
    echo "❌ Erro ao criar backup! Abortando migração."
    exit 1
fi

echo ""
echo "📂 Verificando arquivos de migração SQL..."
MIGRATION_FILE="./src/main/resources/db/migration/V2__add_dam_client_relationships.sql"

if [ ! -f "$MIGRATION_FILE" ]; then
    echo "❌ Arquivo de migração não encontrado: $MIGRATION_FILE"
    exit 1
fi

echo "✅ Arquivo encontrado: $MIGRATION_FILE"

echo ""
echo "📊 Verificando se migração já foi executada..."
ALREADY_MIGRATED=$(docker exec geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} -t -c "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'template_questionnaires' AND column_name = 'dam_id');" | tr -d '[:space:]')

if [ "$ALREADY_MIGRATED" = "t" ]; then
    echo "⚠️  Migração V2 já foi executada anteriormente."
    echo "   A coluna dam_id já existe na tabela template_questionnaires."
    echo ""
    read -p "Deseja forçar re-execução? ATENÇÃO: Isso pode causar ERROS GRAVES! (digite 'FORCAR'): "
    echo
    if [[ ! $REPLY == "FORCAR" ]]; then
        echo "ℹ️  Pulando migração. Use os comandos de verificação abaixo para validar."
        SKIP_MIGRATION=true
    fi
fi

if [ "$SKIP_MIGRATION" != "true" ]; then
    echo ""
    echo "🚀 Executando migração SQL em PRODUÇÃO..."
    docker exec -i geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} < "$MIGRATION_FILE"
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✅ Migração executada com sucesso em PRODUÇÃO!"
    else
        echo ""
        echo "❌ Erro ao executar migração em PRODUÇÃO!"
        echo ""
        echo "🔄 Para restaurar o backup, execute:"
        echo "   docker exec -i geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} < ./logs/${BACKUP_FILE}"
        echo ""
        echo "💡 Verifique os logs acima para mais detalhes"
        exit 1
    fi
fi

if [ $? -eq 0 ] || [ "$SKIP_MIGRATION" = "true" ]; then
    
    echo ""
    echo "🔍 Verificando templates órfãos (sem dam_id):"
    docker exec geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT COUNT(*) as templates_sem_dam FROM template_questionnaires WHERE dam_id IS NULL;"
    
    echo ""
    echo "🔍 Verificando questões órfãs (sem client_id):"
    docker exec geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT COUNT(*) as questoes_sem_client FROM questions WHERE client_id IS NULL;"
    
    echo ""
    echo "📈 Templates por barragem (top 10):"
    docker exec geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT d.name as barragem, COUNT(tq.id) as total_templates FROM dam d LEFT JOIN template_questionnaires tq ON tq.dam_id = d.id GROUP BY d.id, d.name ORDER BY total_templates DESC LIMIT 10;"
    
    echo ""
    echo "📈 Questões por cliente (top 10):"
    docker exec geosegbar-postgres-prod psql -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT c.name as cliente, COUNT(q.id) as total_questoes FROM client c LEFT JOIN questions q ON q.client_id = c.id GROUP BY c.id, c.name ORDER BY total_questoes DESC LIMIT 10;"
    
    echo ""
    echo "💾 Backup disponível em: ./logs/${BACKUP_FILE}"
    echo ""
    echo "✅ Migração de PRODUÇÃO concluída com sucesso!"
else
    echo ""
    echo "❌ Erro ao executar verificações!"
    exit 1
fi
