#!/bin/bash

set -e

echo "🚀 Iniciando deploy da API GeoSegBar em PRODUÇÃO com monitoramento..."

# ✅ CORRIGIDO: Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"  # ✅ Garante que comandos executem da raiz

# Verificar se o arquivo .env.prod existe
if [ ! -f .env.prod ]; then
    echo "❌ Arquivo .env.prod não encontrado!"
    echo "📝 Crie o arquivo .env.prod com as variáveis de produção:"
    echo "   cp .env.example .env.prod"
    echo "   # Edite .env.prod com as configurações de produção"
    exit 1
fi

# Carregar variáveis do .env.prod
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

# Criar diretórios necessários
echo "📁 Criando diretórios necessários..."
mkdir -p ${FILE_UPLOAD_DIR}
mkdir -p ${FILE_PSB_DIR}
mkdir -p ./logs
mkdir -p ./prometheus-prod
mkdir -p ./grafana-prod/provisioning/datasources
mkdir -p ./grafana-prod/provisioning/dashboards
mkdir -p ./grafana-prod/dashboards

# ============================================
# CONFIGURAÇÕES DO PROMETHEUS (PRODUÇÃO)
# ============================================
if [ ! -f ./prometheus-prod/prometheus.yml ]; then
    echo "📝 Criando configuração do Prometheus para produção..."
    cat > ./prometheus-prod/prometheus.yml << 'EOF'
global:
  scrape_interval: 30s
  evaluation_interval: 30s
  external_labels:
    environment: 'production'
    cluster: 'geosegbar'

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

rule_files:
  - 'alerts.yml'

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'geosegbar-api'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['geosegbar-api-prod:9090']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'geosegbar-api-prod'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter-prod:9187']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'postgres-prod'
EOF
    echo "✅ prometheus.yml criado para produção"
fi

if [ ! -f ./prometheus-prod/alerts.yml ]; then
    echo "📝 Criando alertas do Prometheus para produção..."
    cat > ./prometheus-prod/alerts.yml << 'EOF'
groups:
  - name: geosegbar_production_alerts
    interval: 30s
    rules:
      - alert: APIDown
        expr: up{job="geosegbar-api"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "API GeoSegBar PRODUÇÃO está DOWN"
          description: "A API não está respondendo há mais de 2 minutos"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[10m]) > 0.02
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "Taxa de erros 5xx alta em PRODUÇÃO"
          description: "Taxa de erros 5xx acima de 2% nos últimos 10 minutos"

      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Uso de memória heap alto em PRODUÇÃO"
          description: "Uso de memória heap acima de 85%"

      - alert: DatabaseDown
        expr: up{job="postgres"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "PostgreSQL está DOWN"
          description: "Banco de dados PostgreSQL não está respondendo"
EOF
    echo "✅ alerts.yml criado para produção"
fi

echo "📝 Criando datasource do Prometheus para produção..."
cat > ./grafana-prod/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus-prod:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: "30s"
      queryTimeout: "60s"
EOF
echo "✅ Datasource Prometheus configurado para produção"

# Criar configuração de dashboards
if [ ! -f ./grafana-prod/provisioning/dashboards/default.yml ]; then
    echo "📝 Criando configuração de dashboards..."
    cat > ./grafana-prod/provisioning/dashboards/default.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'Default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/dashboards
EOF
    echo "✅ Configuração de dashboards criada"
fi

# Copiar dashboards JSON
if [ -d ./grafana/dashboards ]; then
    mkdir -p ./grafana-prod/dashboards
    cp -r ./grafana/dashboards/*.json ./grafana-prod/dashboards/ 2>/dev/null || true
    echo "✅ Dashboards JSON copiados"
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
      -p ${DB_PORT}:5433 \
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
# POSTGRES EXPORTER
# ============================================
if docker ps -q -f name=postgres-exporter-prod | grep -q .; then
    echo "✅ Postgres Exporter já está rodando"
else
    echo "🔄 Iniciando Postgres Exporter..."
    docker run -d \
      --name postgres-exporter-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      -p 9187:9187 \
      -e DATA_SOURCE_NAME="postgresql://${DB_USERNAME}:${DB_PASSWORD}@postgres-prod:5433/${DB_NAME}?sslmode=disable" \
      prometheuscommunity/postgres-exporter:v0.15.0
    echo "✅ Postgres Exporter iniciado"
fi

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
    echo "📦 Container do Redis não encontrado. Criando..."
    
    if ! docker volume ls -q -f name=redis-prod-data | grep -q .; then
        echo "📦 Criando volume para Redis..."
        docker volume create redis-prod-data
    fi
    
    echo "🚀 Iniciando Redis..."
    docker run -d \
      --name redis-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      -p 6379:6379 \
      -v redis-prod-data:/data \
      redis:7-alpine \
      redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
      
    echo "⏳ Aguardando Redis inicializar..."
    sleep 5
    echo "✅ Redis iniciado"
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
  -e JWT_SECRET="${JWT_SECRET}" \
  -e MAIL_HOST="${MAIL_HOST}" \
  -e MAIL_PORT="${MAIL_PORT}" \
  -e MAIL_USERNAME="${MAIL_USERNAME}" \
  -e MAIL_PASSWORD="${MAIL_PASSWORD}" \
  -e AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
  -e AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
  -e AWS_REGION="${AWS_REGION}" \
  -e AWS_BUCKET_NAME="${AWS_BUCKET_NAME}" \
  -e FILE_UPLOAD_DIR="${FILE_UPLOAD_DIR}" \
  -e FILE_BASE_URL="${FILE_BASE_URL}" \
  -e FILE_PSB_DIR="${FILE_PSB_DIR}" \
  -e FRONTEND_URL="${FRONTEND_URL}" \
  -e ANA_API_IDENTIFIER="${ANA_API_IDENTIFIER}" \
  -e ANA_API_PASSWORD="${ANA_API_PASSWORD}" \
  -e ANA_API_AUTH_URL="${ANA_API_AUTH_URL}" \
  -e ANA_API_TELEMETRY_URL="${ANA_API_TELEMETRY_URL}" \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
  -e TZ="${TZ}" \
  -v ${FILE_UPLOAD_DIR}:${FILE_UPLOAD_DIR} \
  -v $SCRIPT_DIR/logs:/app/logs \
  geosegbar-prod:latest

echo "⏳ Aguardando aplicação inicializar..."
sleep 30

# ============================================
# PROMETHEUS
# ============================================
if docker ps -q -f name=prometheus-prod | grep -q .; then
    echo "🔄 Reiniciando Prometheus..."
    docker stop prometheus-prod
    docker rm prometheus-prod
fi

if ! docker volume ls -q -f name=prometheus-prod-data | grep -q .; then
    docker volume create prometheus-prod-data
fi

echo "🚀 Iniciando Prometheus..."
docker run -d \
  --name prometheus-prod \
  --restart unless-stopped \
  --network geosegbar-network \
  -p 9091:9090 \
  -v $SCRIPT_DIR/prometheus-prod/prometheus.yml:/etc/prometheus/prometheus.yml:ro \
  -v $SCRIPT_DIR/prometheus-prod/alerts.yml:/etc/prometheus/alerts.yml:ro \
  -v prometheus-prod-data:/prometheus \
  prom/prometheus:v2.48.0 \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --web.console.libraries=/etc/prometheus/console_libraries \
  --web.console.templates=/etc/prometheus/consoles \
  --web.enable-lifecycle \
  --storage.tsdb.retention.time=${PROMETHEUS_RETENTION:-30d}

# ============================================
# GRAFANA
# ============================================
if docker ps -q -f name=grafana-prod | grep -q .; then
    echo "🔄 Reiniciando Grafana..."
    docker stop grafana-prod
    docker rm grafana-prod
fi

if ! docker volume ls -q -f name=grafana-prod-data | grep -q .; then
    docker volume create grafana-prod-data
fi

echo "🚀 Iniciando Grafana..."
docker run -d \
  --name grafana-prod \
  --restart unless-stopped \
  --network geosegbar-network \
  -p 3001:3000 \
  -e GF_SECURITY_ADMIN_USER=admin \
  -e GF_SECURITY_ADMIN_PASSWORD="${GRAFANA_PASSWORD}" \
  -e GF_INSTALL_PLUGINS=redis-datasource \
  -e GF_SERVER_ROOT_URL=http://localhost:3001 \
  -e GF_USERS_ALLOW_SIGN_UP=false \
  -v grafana-prod-data:/var/lib/grafana \
  -v $SCRIPT_DIR/grafana-prod/provisioning:/etc/grafana/provisioning:ro \
  -v $SCRIPT_DIR/grafana-prod/dashboards:/etc/grafana/dashboards:ro \
  grafana/grafana:10.2.2

echo "⏳ Aguardando monitoramento inicializar..."
sleep 10

# ============================================
# VERIFICAÇÃO
# ============================================
echo "🔍 Verificando status da aplicação..."
if curl -f http://localhost:${SERVER_PORT}/actuator/health > /dev/null 2>&1; then
    echo "✅ Deploy em PRODUÇÃO realizado com sucesso!"
    echo ""
    echo "📡 SERVIÇOS DISPONÍVEIS:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "🌐 API:           http://localhost:${SERVER_PORT}"
    echo "📊 Prometheus:    http://localhost:9091"
    echo "📈 Grafana:       http://localhost:3001 (admin / ${GRAFANA_PASSWORD})"
    echo "🗄️  PostgreSQL:    localhost:${DB_PORT}"

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📊 Status dos containers:"
    docker ps --filter "name=geosegbar" --filter "name=postgres-prod" --filter "name=prometheus-prod" --filter "name=grafana-prod"
    
    echo ""
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

echo ""
echo "🎉 Deploy em PRODUÇÃO concluído com monitoramento completo!"