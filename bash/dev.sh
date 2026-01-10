#!/bin/bash

set -e

echo "🚀 Iniciando ambiente de desenvolvimento com monitoramento..."

# ✅ CORRIGIDO: Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$SCRIPT_DIR"  # ✅ Garante que comandos executem da raiz

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
mkdir -p ./prometheus
mkdir -p ./grafana/provisioning/datasources
mkdir -p ./grafana/provisioning/dashboards
mkdir -p ./grafana/dashboards

# Verificar se arquivos de configuração do Prometheus existem
if [ ! -f ./prometheus/prometheus.yml ]; then
    echo "⚠️  Arquivo prometheus.yml não encontrado!"
    echo "📝 Criando configuração padrão do Prometheus..."
    cat > ./prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    environment: 'dev'
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
        replacement: 'geosegbar-api-dev'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'postgres-dev'
EOF
    echo "✅ prometheus.yml criado"
fi

if [ ! -f ./prometheus/alerts.yml ]; then
    echo "📝 Criando arquivo de alertas do Prometheus..."
    cat > ./prometheus/alerts.yml << 'EOF'
groups:
  - name: geosegbar_alerts
    interval: 30s
    rules:
      - alert: APIDown
        expr: up{job="geosegbar-api"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "API GeoSegBar está DOWN"
          description: "A API não está respondendo há mais de 1 minuto"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Taxa de erros 5xx alta"
          description: "Taxa de erros 5xx acima de 5% nos últimos 5 minutos"

      - alert: HighMemoryUsage
        expr: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Uso de memória heap alto"
          description: "Uso de memória heap acima de 90%"
EOF
    echo "✅ alerts.yml criado"
fi

# Verificar se provisioning do Grafana existe
if [ ! -f ./grafana/provisioning/datasources/prometheus.yml ]; then
    echo "📝 Criando datasource do Prometheus no Grafana..."
    cat > ./grafana/provisioning/datasources/prometheus.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: "15s"
EOF
    echo "✅ Datasource Prometheus configurado"
fi

if [ ! -f ./grafana/provisioning/dashboards/default.yml ]; then
    echo "📝 Criando configuração de dashboards do Grafana..."
    cat > ./grafana/provisioning/dashboards/default.yml << 'EOF'
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

# Parar containers antigos
echo "🛑 Parando containers anteriores (se existirem)..."
docker-compose down 2>/dev/null || true

# Rebuild da imagem (força sem cache)
echo "🔨 Construindo imagem da aplicação..."
docker-compose build --no-cache geosegbar-api

# Iniciar com docker-compose
echo "🚀 Iniciando todos os serviços..."
docker-compose up -d

echo ""
echo "⏳ Aguardando serviços inicializarem..."
sleep 15

# Verificar status dos containers
echo ""
echo "📊 Status dos containers:"
docker-compose ps

echo ""
echo "✅ Ambiente de desenvolvimento iniciado com sucesso!"
echo ""
echo "📡 SERVIÇOS DISPONÍVEIS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 API:           http://localhost:${SERVER_PORT}"
echo "📊 Prometheus:    http://localhost:9091"
echo "📈 Grafana:       http://localhost:3001 (admin / ${GRAFANA_PASSWORD})"
echo "🗄️  PostgreSQL:    localhost:${DB_PORT}"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 COMANDOS ÚTEIS:"
echo "  Ver logs da API:        docker-compose logs -f geosegbar-api"
echo "  Ver logs do Prometheus: docker-compose logs -f prometheus"
echo "  Ver logs do Grafana:    docker-compose logs -f grafana"
echo "  Ver todos os logs:      docker-compose logs -f"
echo "  Parar tudo:             docker-compose down"
echo "  Parar e limpar volumes: docker-compose down -v"
echo ""
echo "🔍 HEALTH CHECKS:"
echo "  API Health:      curl http://localhost:${SERVER_PORT}/actuator/health"
echo "  API Metrics:     curl http://localhost:${SERVER_PORT}/actuator/prometheus"
echo "  Prometheus UI:   http://localhost:9091/targets"
echo ""

# Aguardar API ficar healthy
echo "⏳ Aguardando API ficar healthy (timeout: 60s)..."
TIMEOUT=60
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if curl -f http://localhost:${SERVER_PORT}/actuator/health > /dev/null 2>&1; then
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
docker-compose logs --tail 30 geosegbar-api
echo ""
echo "💡 Para continuar verificando: docker-compose logs -f geosegbar-api"