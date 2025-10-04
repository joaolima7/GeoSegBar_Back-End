#!/bin/bash

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
if ! docker ps -q -f name=postgres-prod | grep -q .; then
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
    sleep 10
    
    # Verificar se precisamos migrar dados do banco antigo
    if [ "$1" == "--migrate" ]; then
        echo "🔄 Migrando dados do banco antigo..."
        
        # Instalar cliente PostgreSQL se necessário
        if ! command -v pg_dump &> /dev/null; then
            echo "⚙️ Instalando cliente PostgreSQL..."
            apt-get update && apt-get install -y postgresql-client
        fi
        
        # Exportar dados do banco antigo
        echo "📤 Exportando dados do banco antigo..."
        PGPASSWORD=Geometr!s@ pg_dump -h 162.240.165.193 -U postgres -d wwgeom_dev_test > /tmp/db_export.sql
        
        # Importar para o novo banco
        echo "📥 Importando dados para o novo banco..."
        cat /tmp/db_export.sql | docker exec -i postgres-prod psql -U postgres -d geosegbar_prod
        
        # Limpar arquivo temporário
        rm /tmp/db_export.sql
        
        echo "✅ Migração de dados concluída!"
    fi
else
    echo "✅ Banco de dados já está rodando"
fi

# Fazer backup do container da API atual
echo "📦 Fazendo backup do container atual da API..."
docker stop geosegbar-api-prod || true
docker rename geosegbar-api-prod geosegbar-api-backup-$(date +%Y%m%d-%H%M%S) || true

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
if curl -f http://localhost:9090/actuator/health > /dev/null 2>&1; then
    echo "✅ Deploy em PRODUÇÃO realizado com sucesso!"
    
    # Remover container de backup após sucesso
    docker rm geosegbar-api-backup-* 2>/dev/null || true
    
    # Limpar imagens não utilizadas
    docker image prune -f
else
    echo "❌ Falha no deploy! Restaurando backup..."
    docker stop geosegbar-api-prod || true
    docker rm geosegbar-api-prod || true
    
    # Restaurar backup se existir
    BACKUP_CONTAINER=$(docker ps -a --filter "name=geosegbar-api-backup-" --format "{{.Names}}" | head -1)
    if [ ! -z "$BACKUP_CONTAINER" ]; then
        docker rename $BACKUP_CONTAINER geosegbar-api-prod
        docker start geosegbar-api-prod
        echo "🔄 Backup restaurado"
    fi
    exit 1
fi

echo "🎉 Deploy em PRODUÇÃO concluído!"