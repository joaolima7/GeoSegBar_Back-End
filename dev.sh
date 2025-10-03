#!/bin/bash
# filepath: dev.sh

echo "🚀 Iniciando ambiente de desenvolvimento..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Criar diretórios necessários
mkdir -p ./storage-dev/psb ./logs

# Iniciar com docker-compose
docker-compose up -d

echo "✅ Ambiente de desenvolvimento iniciado!"
echo "📊 Para ver logs: docker-compose logs -f geosegbar-api"
echo "🛑 Para parar: docker-compose down"