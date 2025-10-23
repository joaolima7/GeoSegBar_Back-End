#!/bin/bash

# ==============================================
# SCRIPT PARA RESET COMPLETO DO BANCO DE DADOS
# ==============================================

echo "⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️"
echo "⚠️                                      ⚠️"
echo "⚠️  ATENÇÃO! ESTA OPERAÇÃO IRÁ EXCLUIR  ⚠️"
echo "⚠️  TODOS OS DADOS DE PRODUÇÃO!         ⚠️"
echo "⚠️                                      ⚠️"
echo "⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️⚠️"
echo ""
echo "Esta operação irá:"
echo "  1. Parar a aplicação em execução"
echo "  2. Excluir o banco de dados atual"
echo "  3. Criar um novo banco de dados vazio"
echo "  4. Reiniciar a aplicação (que realizará a criação das tabelas)"
echo ""
echo "⚠️ RECOMENDAÇÃO: Execute ./backup_database_prod.sh antes para criar um backup!"
echo ""
read -p "Digite 'RESET' para confirmar esta operação: " confirmacao

if [ "$confirmacao" != "RESET" ]; then
    echo "Operação cancelada."
    exit 1
fi

# Carregar variáveis de ambiente de produção
set -a
source .env.prod
set +a

echo "🛑 Parando a aplicação..."
docker stop geosegbar-api-prod || echo "   Container não estava rodando"
docker rm geosegbar-api-prod || echo "   Container não existia"

echo "🗑️ Eliminando o banco de dados atual..."
docker exec postgres-prod psql -U ${DB_USERNAME} -c "DROP DATABASE ${DB_NAME};"

echo "🆕 Criando um novo banco de dados vazio..."
docker exec postgres-prod psql -U ${DB_USERNAME} -c "CREATE DATABASE ${DB_NAME};"

echo "🔄 Limpando o cache Redis..."
docker exec redis-prod redis-cli FLUSHALL

echo "🚀 Recriando o container da aplicação..."

# ============================================
# Reconstruir e iniciar a aplicação diretamente
# ============================================
echo "🔨 Construindo nova imagem Docker..."
docker build -t geosegbar-prod:latest .

echo "🚀 Subindo novo container da API..."
docker run -d \
  --name geosegbar-api-prod \
  --restart unless-stopped \
  --network geosegbar-network \
  -p ${SERVER_PORT}:9090 \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
  -e JAVA_OPTS="${JAVA_OPTS}" \
  -e DB_HOST="${DB_HOST}" \
  -e DB_PORT="${DB_PORT}" \
  -e DB_NAME="${DB_NAME}" \
  -e DB_USERNAME="${DB_USERNAME}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e JWT_SECRET="${JWT_SECRET}" \
  -e MAIL_HOST="${MAIL_HOST}" \
  -e MAIL_PORT="${MAIL_PORT}" \
  -e MAIL_USERNAME="${MAIL_USERNAME}" \
  -e MAIL_PASSWORD="${MAIL_PASSWORD}" \
  -e FILE_UPLOAD_DIR="${FILE_UPLOAD_DIR}" \
  -e FILE_BASE_URL="${FILE_BASE_URL}" \
  -e FILE_PSB_DIR="${FILE_PSB_DIR}" \
  -e FRONTEND_URL="${FRONTEND_URL}" \
  -e ANA_API_IDENTIFIER="${ANA_API_IDENTIFIER}" \
  -e ANA_API_PASSWORD="${ANA_API_PASSWORD}" \
  -e ANA_API_AUTH_URL="${ANA_API_AUTH_URL}" \
  -e ANA_API_TELEMETRY_URL="${ANA_API_TELEMETRY_URL}" \
  -e TZ="${TZ}" \
  -v ${FILE_UPLOAD_DIR}:${FILE_UPLOAD_DIR} \
  -v $(pwd)/logs:/app/logs \
  geosegbar-prod:latest

echo "⏳ Aguardando aplicação inicializar (60s)..."
sleep 60

# Verificação com mais detalhes
echo "🔍 Verificando status da aplicação..."
if curl -f http://localhost:${SERVER_PORT}/actuator/health 2>/dev/null | grep -q "UP"; then
    echo "✅ Aplicação iniciada com sucesso!"
    echo "🌐 API disponível em: http://localhost:${SERVER_PORT}"
    echo "📊 Banco de dados: ${DB_NAME} (resetado)"
    
    echo "📋 Status dos containers:"
    docker ps --filter "name=geosegbar" --filter "name=postgres-prod" --filter "name=redis-prod"
    
    docker image prune -f > /dev/null 2>&1 || true
    
    echo "🎉 Reset do banco de dados concluído com sucesso!"
else
    echo "❌ Falha ao iniciar a aplicação!"
    echo "📋 Últimas linhas do log da API:"
    docker logs --tail 30 geosegbar-api-prod
    echo ""
    echo "🔍 Status do container:"
    docker ps -a --filter "name=geosegbar-api-prod"
    echo ""
    echo "💡 O banco foi resetado, mas a aplicação não iniciou corretamente."
    echo "💡 Verifique os logs completos: docker logs geosegbar-api-prod"
    exit 1
fi