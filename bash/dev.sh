#!/bin/bash

set -e

echo "🚀 Iniciando ambiente de desenvolvimento..."

# Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"  # Garante que comandos executem da raiz

# Verificar se o arquivo .env existe
if [ ! -f .env ]; then
    echo "❌ Arquivo .env não encontrado!"
    echo "📝 Copie o arquivo .env.example para .env e configure as variáveis:"
    echo "   cp .env.example .env"
    exit 1
fi

# Carregar variáveis do .env
set -a
source .env
set +a

echo "📦 Carregando variáveis de ambiente do arquivo .env..."
echo "🔧 Profile ativo: ${SPRING_PROFILES_ACTIVE}"

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker não está rodando!"
    exit 1
fi

# Criar diretórios necessários
echo "📁 Criando diretórios necessários..."
mkdir -p ./storage/uploads/psb
mkdir -p ./logs

# O monitoramento (Prometheus, Grafana, Loki, Tempo, Alloy) não sobe mais por
# aqui: ele mora em monitoring/ e tem compose próprio. Ver o fim deste script e
# docs/MONITORAMENTO.md, seção "Desenvolvimento local".

# Parar containers antigos
echo "🛑 Parando containers anteriores (se existirem)..."
docker compose down 2>/dev/null || true

# Rebuild da imagem (força sem cache)
echo "🔨 Construindo imagem da aplicação..."
docker compose build --no-cache geosegbar-api

# Iniciar com docker compose
echo "🚀 Iniciando todos os serviços..."
docker compose up -d

echo ""
echo "⏳ Aguardando serviços inicializarem..."
sleep 15

# Verificar status dos containers
echo ""
echo "📊 Status dos containers:"
docker compose ps

echo ""
echo "✅ Ambiente de desenvolvimento iniciado com sucesso!"
echo ""
echo "📡 SERVIÇOS DISPONÍVEIS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 API (nginx):   http://localhost:${SERVER_PORT}"
echo "🗄️  PostgreSQL:    localhost:${DB_PORT}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 COMANDOS ÚTEIS:"
echo "  Ver logs da API:        docker compose logs -f geosegbar-api"
echo "  Ver todos os logs:      docker compose logs -f"
echo "  Parar tudo:             docker compose down"
echo "  Parar e limpar volumes: docker compose down -v"
echo ""
echo "🔍 HEALTH CHECKS (actuator na porta de management 8081, só dentro do container):"
echo "  API Health:      docker compose exec -T geosegbar-api wget -qO- http://localhost:8081/actuator/health"
echo "  API Metrics:     docker compose exec -T geosegbar-api wget -qO- http://localhost:8081/actuator/prometheus"
echo ""
echo "📈 MONITORAMENTO (opcional, Grafana em http://localhost:3000):"
echo "  cp monitoring/.env.dev.example monitoring/.env.dev && ./monitoring/scripts/dev-secrets.sh"
echo "  docker compose --env-file monitoring/.env.dev -f monitoring/docker-compose.yml -f monitoring/docker-compose.dev.yml up -d"
echo ""

# Aguardar API ficar healthy. O nginx bloqueia /actuator/ e a porta 8081 não é
# publicada: a sonda roda dentro do container, como o HEALTHCHECK da imagem.
echo "⏳ Aguardando API ficar healthy (timeout: 60s)..."
TIMEOUT=60
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if docker compose exec -T geosegbar-api \
         wget -q -O /dev/null http://localhost:8081/actuator/health/liveness > /dev/null 2>&1; then
        echo "✅ API está HEALTHY!"
        echo ""
        echo "🎉 Ambiente pronto para desenvolvimento!"
        exit 0
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo "   Aguardando... (${ELAPSED}s/${TIMEOUT}s)"
done

echo ""
echo "⚠️  API não ficou healthy no tempo esperado."
echo "📋 Verificando logs da API:"
docker compose logs --tail 30 geosegbar-api
echo ""
echo "💡 Para continuar verificando: docker compose logs -f geosegbar-api"
