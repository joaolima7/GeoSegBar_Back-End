#!/bin/bash
# filepath: /Users/joaoremonato/Projects/SpringBoot/GeoSegBar_Back-End/deploy.sh

set -e

echo "🚀 Iniciando deploy da API GeoSegBar..."

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Fazer backup do container atual
echo "📦 Fazendo backup do container atual..."
docker stop geosegbar-api || true
docker rename geosegbar-api geosegbar-api-backup-$(date +%Y%m%d-%H%M%S) || true

# Fazer pull das mudanças
echo "📥 Atualizando código..."
git pull origin main

# Construir nova imagem
echo "🔨 Construindo nova imagem Docker..."
docker build -t geosegbar:latest .

# Subir novo container
echo "🚀 Subindo novo container..."
docker run -d \
  --name geosegbar-api \
  --restart unless-stopped \
  -p 9090:9090 \
  -v $(pwd)/storage:/app/storage \
  -v $(pwd)/logs:/app/logs \
  geosegbar:latest

# Aguardar aplicação ficar pronta
echo "⏳ Aguardando aplicação inicializar..."
sleep 30

# Verificar se a aplicação está rodando
if curl -f http://localhost:9090/actuator/health > /dev/null 2>&1; then
    echo "✅ Deploy realizado com sucesso!"
    
    # Remover container de backup após sucesso
    docker rm geosegbar-api-backup-* 2>/dev/null || true
    
    # Limpar imagens não utilizadas
    docker image prune -f
else
    echo "❌ Falha no deploy! Restaurando backup..."
    docker stop geosegbar-api || true
    docker rm geosegbar-api || true
    
    # Restaurar backup se existir
    BACKUP_CONTAINER=$(docker ps -a --filter "name=geosegbar-api-backup-" --format "{{.Names}}" | head -1)
    if [ ! -z "$BACKUP_CONTAINER" ]; then
        docker rename $BACKUP_CONTAINER geosegbar-api
        docker start geosegbar-api
        echo "🔄 Backup restaurado"
    fi
    exit 1
fi

echo "🎉 Deploy concluído!"