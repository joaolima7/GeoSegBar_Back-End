#!/bin/bash

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Deploy GeoSegBar em PRODUÇÃO com Docker Compose${NC}"
echo ""

# Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"

# Verificar arquivo .env.prod
if [ ! -f .env.prod ]; then
    echo -e "${RED}❌ Arquivo .env.prod não encontrado!${NC}"
    echo "📝 Crie o arquivo .env.prod com as variáveis de produção:"
    echo "   cp .env.example .env.prod"
    echo "   # Edite .env.prod com as configurações de produção"
    exit 1
fi

# Carregar variáveis
set -a
source .env.prod
set +a

echo -e "${BLUE}📦 Carregando variáveis de ambiente...${NC}"
echo "   Profile: ${SPRING_PROFILES_ACTIVE}"
echo "   Banco: ${DB_NAME}"
echo "   Frontend URL: ${FRONTEND_URL}"

# Verificar se Docker está rodando
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker não está rodando!${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker está operacional${NC}"

# Criar diretórios necessários
mkdir -p ./logs
mkdir -p ./prometheus-prod
mkdir -p ./grafana-prod/provisioning/datasources
mkdir -p ./grafana-prod/provisioning/dashboards
mkdir -p ./grafana-prod/dashboards

# Criar configuração do Prometheus se não existir
if [ ! -f ./prometheus-prod/prometheus.yml ]; then
    echo -e "${BLUE}📝 Criando configuração do Prometheus...${NC}"
    cat > ./prometheus-prod/prometheus.yml << 'EOF'
global:
  scrape_interval: 30s
  evaluation_interval: 30s
  external_labels:
    environment: 'prod'
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
      - targets: ['geosegbar-api:9090']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'api-prod'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'postgres-prod'
EOF
    echo -e "${GREEN}✅ prometheus.yml criado${NC}"
fi

# Criar alertas do Prometheus
if [ ! -f ./prometheus-prod/alerts.yml ]; then
    echo -e "${BLUE}📝 Criando arquivo de alertas...${NC}"
    cat > ./prometheus-prod/alerts.yml << 'EOF'
groups:
  - name: geosegbar_prod_alerts
    interval: 30s
    rules:
      - alert: APIDown
        expr: up{job="geosegbar-api"} == 0
        for: 2m
        annotations:
          summary: "API está DOWN"

      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "Taxa alta de erros"

      - alert: HighMemoryUsage
        expr: container_memory_usage_bytes{name="geosegbar-api-prod"} / container_spec_memory_limit_bytes > 0.8
        for: 5m
        annotations:
          summary: "Uso alto de memória na API"

      - alert: DatabaseDown
        expr: up{job="postgres"} == 0
        for: 2m
        annotations:
          summary: "Banco de dados está DOWN"
EOF
    echo -e "${GREEN}✅ alerts.yml criado${NC}"
fi

# Configurar Grafana datasource
echo -e "${BLUE}📝 Configurando datasource do Grafana...${NC}"
cat > ./grafana-prod/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: "30s"
EOF
echo -e "${GREEN}✅ Datasource configurado${NC}"

# Configurar dashboards do Grafana
if [ ! -f ./grafana-prod/provisioning/dashboards/default.yml ]; then
    echo -e "${BLUE}📝 Configurando provisioning de dashboards...${NC}"
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
    echo -e "${GREEN}✅ Provisioning de dashboards configurado${NC}"
fi

# Build da imagem Docker
echo -e "${BLUE}🔨 Construindo imagem Docker...${NC}"
docker-compose -f docker-compose.prod.yml build geosegbar-api

# Iniciar serviços com docker-compose
echo -e "${BLUE}🚀 Iniciando serviços com Docker Compose...${NC}"
docker-compose -f docker-compose.prod.yml up -d

echo -e "${BLUE}⏳ Aguardando serviços inicializarem...${NC}"
sleep 20

# Verificar status
echo -e "${BLUE}📊 Status dos containers:${NC}"
docker-compose -f docker-compose.prod.yml ps

# Aguardar API ficar healthy
echo -e "${BLUE}⏳ Aguardando API ficar healthy...${NC}"
TIMEOUT=120
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if curl -sf http://localhost:${SERVER_PORT}/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ API está HEALTHY!${NC}"
        break
    fi
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    echo "   Tentativa $((ELAPSED/5))... (${ELAPSED}s/${TIMEOUT}s)"
done

if [ $ELAPSED -ge $TIMEOUT ]; then
    echo -e "${RED}❌ API não ficou healthy no tempo esperado${NC}"
    echo -e "${YELLOW}📋 Verificando logs:${NC}"
    docker-compose -f docker-compose.prod.yml logs --tail 50 geosegbar-api
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 Deploy concluído com sucesso!${NC}"
echo ""
echo -e "${BLUE}📡 SERVIÇOS DISPONÍVEIS:${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "  🌐 API:           ${GREEN}http://localhost:${SERVER_PORT}${NC}"
echo -e "  📊 Prometheus:    ${GREEN}http://localhost:9091${NC}"
echo -e "  📈 Grafana:       ${GREEN}http://localhost:3001${NC} (admin / ${GRAFANA_PASSWORD})"
echo -e "  🗄️  PostgreSQL:    ${GREEN}localhost:${DB_PORT}${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo -e "${BLUE}📋 COMANDOS ÚTEIS:${NC}"
echo "  Ver logs da API:        docker-compose -f docker-compose.prod.yml logs -f geosegbar-api"
echo "  Ver logs do Prometheus: docker-compose -f docker-compose.prod.yml logs -f prometheus"
echo "  Ver todos os logs:      docker-compose -f docker-compose.prod.yml logs -f"
echo "  Ver status:             docker-compose -f docker-compose.prod.yml ps"
echo "  Parar serviços:         docker-compose -f docker-compose.prod.yml stop"
echo "  Reiniciar API:          docker-compose -f docker-compose.prod.yml restart geosegbar-api"
echo ""
echo -e "${BLUE}🔍 HEALTH CHECKS:${NC}"
echo "  API Health:      curl http://localhost:${SERVER_PORT}/actuator/health"
echo "  API Metrics:     curl http://localhost:${SERVER_PORT}/actuator/prometheus"
echo "  Prometheus UI:   http://localhost:9091/targets"
echo ""
