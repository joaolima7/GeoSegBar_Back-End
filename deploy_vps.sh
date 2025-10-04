#!/bin/bash
# filepath: /Users/joaoremonato/Projects/SpringBoot/GeoSegBar_Back-End/deploy_vps.sh

set -e

echo "🚀 Iniciando deploy da API GeoSegBar em PRODUÇÃO..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Criar rede se não existir
docker network create geosegbar-network 2>/dev/null || true

# Verificar se o banco de dados existe e está rodando
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
    
    # Verificar se existe um volume para o banco de dados
    if ! docker volume ls -q -f name=postgres-prod-data | grep -q .; then
        echo "📦 Criando volume para banco de dados..."
        docker volume create postgres-prod-data
    fi
    
    # Iniciar container do banco
    echo "🚀 Iniciando banco de dados PostgreSQL..."
    docker run -d \
      --name postgres-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      -p 5433:5432 \
      -e POSTGRES_DB=geosegbar_prod \
      -e POSTGRES_USER=postgres \
      -e POSTGRES_PASSWORD=Geometr!s@ \
      -v postgres-prod-data:/var/lib/postgresql/data \
      postgres:16-alpine
      
    echo "⏳ Aguardando banco de dados inicializar..."
    sleep 15
fi

# Parar e remover container atual da API (se existir)
echo "🛑 Parando container atual da API..."
docker stop geosegbar-api-prod 2>/dev/null || echo "   Container não estava rodando"
docker rm geosegbar-api-prod 2>/dev/null || echo "   Container não existia"

# Fazer pull das mudanças
echo "📥 Atualizando código..."
git pull origin main

# Construir nova imagem
echo "🔨 Construindo nova imagem Docker..."
docker build -t geosegbar-prod:latest .

# Subir novo container da API
echo "🚀 Subindo novo container da API..."
docker run -d \
  --name geosegbar-api-prod \
  --restart unless-stopped \
  --network geosegbar-network \
  -p 9090:9090 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC" \
  -v /home/wwgeomprod/public_html/storage/app/public:/home/wwgeomprod/public_html/storage/app/public \
  -v $(pwd)/logs:/app/logs \
  geosegbar-prod:latest

# Aguardar aplicação ficar pronta
echo "⏳ Aguardando aplicação inicializar..."
sleep 30

# Verificar se a aplicação está rodando
echo "🔍 Verificando status da aplicação..."
if curl -f http://localhost:9090/actuator/health > /dev/null 2>&1; then
    echo "✅ Deploy em PRODUÇÃO realizado com sucesso!"
    echo "🌐 API disponível em: http://localhost:9090"
    
    # Mostrar status dos containers
    echo "📊 Status dos containers:"
    docker ps --filter "name=geosegbar" --filter "name=postgres-prod"
    
    # Limpar imagens não utilizadas
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