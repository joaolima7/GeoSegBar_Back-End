#!/bin/bash

echo "🚀 Iniciando ambiente de desenvolvimento..."

# Verificar se o arquivo .env existe
if [ ! -f .env ]; then
    echo "❌ Arquivo .env não encontrado!"
    echo "📝 Copie o arquivo .env.example para .env e configure as variáveis:"
    echo "   cp .env.example .env"
    exit 1
fi

# Carregar variáveis do .env
export $(cat .env | grep -v '^#' | xargs)

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Criar diretórios necessários
mkdir -p ./storage-dev/psb ./logs

echo "📦 Carregando variáveis de ambiente do arquivo .env..."
echo "🔧 Profile ativo: ${SPRING_PROFILES_ACTIVE}"

# Iniciar com docker-compose
docker-compose --env-file .env up -d

echo "✅ Ambiente de desenvolvimento iniciado!"
echo "🌐 API disponível em: http://localhost:${SERVER_PORT}"
echo "🗄️  PostgreSQL: localhost:${DB_PORT}"
echo "🔴 Redis: localhost:${REDIS_PORT}"
echo ""
echo "📊 Para ver logs: docker-compose logs -f geosegbar-api"
echo "🛑 Para parar: docker-compose down"