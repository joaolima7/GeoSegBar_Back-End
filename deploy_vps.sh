#!/bin/bash

set -e

echo "🚀 Iniciando deploy da API GeoSegBar em PRODUÇÃO..."

# Verificar se o arquivo .env.prod existe
if [ ! -f .env.prod ]; then
    echo "❌ Arquivo .env.prod não encontrado!"
    echo "📝 Crie o arquivo .env.prod com as variáveis de produção:"
    echo "   cp .env.example .env.prod"
    echo "   # Edite .env.prod com as configurações de produção"
    exit 1
fi

# Carregar variáveis do .env.prod (método correto para lidar com valores com espaços)
set -a
source .env.prod
set +a

echo "📦 Carregando variáveis de ambiente do arquivo .env.prod..."
echo "🔧 Profile ativo: ${SPRING_PROFILES_ACTIVE}"

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Criar rede se não existir
docker network create geosegbar-network 2>/dev/null || true

# ============================================
# REDIS
# ============================================
if docker ps -q -f name=redis-prod | grep -q .; then
    echo "✅ Redis já está rodando"
elif docker ps -a -q -f name=redis-prod | grep -q .; then
    echo "🔄 Container do Redis existe mas está parado. Reiniciando..."
    docker start redis-prod
    echo "✅ Redis reiniciado"
else
    echo "🔄 Container do Redis não encontrado. Criando..."
    
    if ! docker volume ls -q -f name=redis-prod-data | grep -q .; then
        echo "📦 Criando volume para Redis..."
        docker volume create redis-prod-data
    fi
    
    echo "🚀 Iniciando Redis..."
    docker run -d \
      --name redis-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      -p ${REDIS_PORT}:6379 \
      -v redis-prod-data:/data \
      redis:7-alpine redis-server --save 60 1 --loglevel warning --maxmemory ${REDIS_MAXMEMORY} --maxmemory-policy volatile-lru
      
    echo "⏳ Aguardando Redis inicializar..."
    sleep 5
fi

# ============================================
# POSTGRESQL
# ============================================
if docker ps -q -f name=postgres-prod | grep -q .; then
    echo "✅ Banco de dados já está rodando"
elif docker ps -a -q -f name=postgres-prod | grep -q .; then
    echo "🔄 Container do banco existe mas está parado. Reiniciando..."
    docker start postgres-prod
    echo "⏳ Aguardando banco de dados inicializar..."
    sleep 10
    echo "✅ Banco de dados reiniciado"
else
    echo "🛢️ Container do banco de dados não encontrado. Criando..."
    
    if ! docker volume ls -q -f name=postgres-prod-data | grep -q .; then
        echo "📦 Criando volume para banco de dados..."
        docker volume create postgres-prod-data
    fi
    
    echo "🚀 Iniciando banco de dados PostgreSQL..."
    docker run -d \
      --name postgres-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      -p ${DB_PORT}:5432 \
      -e POSTGRES_DB=${DB_NAME} \
      -e POSTGRES_USER=${DB_USERNAME} \
      -e POSTGRES_PASSWORD=${DB_PASSWORD} \
      -e TZ=${TZ} \
      -v postgres-prod-data:/var/lib/postgresql/data \
      postgres:16-alpine
      
    echo "⏳ Aguardando banco de dados inicializar..."
    sleep 15
fi

# ============================================
# APPLICATION
# ============================================
echo "🛑 Parando container atual da API..."
docker stop geosegbar-api-prod 2>/dev/null || echo "   Container não estava rodando"
docker rm geosegbar-api-prod 2>/dev/null || echo "   Container não existia"

echo "📥 Atualizando código..."
git pull origin main

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

echo "⏳ Aguardando aplicação inicializar..."
sleep 30

echo "🔍 Verificando status da aplicação..."
if curl -f http://localhost:${SERVER_PORT}/actuator/health > /dev/null 2>&1; then
    echo "✅ Deploy em PRODUÇÃO realizado com sucesso!"
    echo "🌐 API disponível em: http://localhost:${SERVER_PORT}"
    
    echo "📊 Status dos containers:"
    docker ps --filter "name=geosegbar" --filter "name=postgres-prod" --filter "name=redis-prod"
    
    echo "🧹 Limpando imagens não utilizadas..."
    docker image prune -f > /dev/null 2>&1 || true
    
else
    echo "❌ Falha no deploy! Verificando logs..."
    echo "📋 Últimas linhas do log da API:"
    docker logs --tail 20 geosegbar-api-prod
    echo ""
    echo "🔍 Status do container:"
    docker ps -a --filter "name=geosegbar-api-prod"
    echo ""
    echo "💡 Para verificar logs completos: docker logs geosegbar-api-prod"
    exit 1
fi

echo "🎉 Deploy em PRODUÇÃO concluído!"