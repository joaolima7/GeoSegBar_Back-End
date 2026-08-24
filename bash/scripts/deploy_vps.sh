#!/bin/bash

set -e

if [ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

echo "🚀 Iniciando deploy da API GeoSegBar em PRODUÇÃO com monitoramento..."

DEPLOY_MODE="${DEPLOY_MODE:-FULL}"
SKIP_GIT_PULL="${SKIP_GIT_PULL:-false}"

if [ "$DEPLOY_MODE" != "FULL" ] && [ "$DEPLOY_MODE" != "DB_ONLY" ]; then
  echo "❌ DEPLOY_MODE inválido: $DEPLOY_MODE (use FULL ou DB_ONLY)"
  exit 1
fi

echo "🧭 Modo de deploy: ${DEPLOY_MODE}"

# ✅ CORRIGIDO: Define o diretório raiz do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
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
      - targets: ['prometheus-prod:9090']

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
POSTGRES_RUNNING=$(docker ps --format '{{.Names}}' | grep -q '^postgres-prod$' && echo "yes" || echo "no")
POSTGRES_EXISTS=$(docker ps -a --format '{{.Names}}' | grep -q '^postgres-prod$' && echo "yes" || echo "no")

# Garante que postgres-prod está na rede correta
if [ "$POSTGRES_EXISTS" = "yes" ]; then
    if ! docker inspect postgres-prod --format '{{range $net, $v := .NetworkSettings.Networks}}{{$net}} {{end}}' 2>/dev/null | grep -qw 'geosegbar-network'; then
        echo "⚠️ postgres-prod não está na geosegbar-network. Conectando..."
        docker network connect geosegbar-network postgres-prod 2>/dev/null || true
    fi
fi

if [ "$POSTGRES_RUNNING" = "yes" ]; then
    echo "✅ Banco de dados já está rodando"
elif [ "$POSTGRES_EXISTS" = "yes" ]; then
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
      -e POSTGRES_DB=${DB_NAME} \
      -e POSTGRES_USER=${DB_USERNAME} \
      -e POSTGRES_PASSWORD=${DB_PASSWORD} \
      -e TZ=${TZ} \
      -v postgres-prod-data:/var/lib/postgresql/data \
      --memory=4g \
      --cpus="2" \
      postgres:16-alpine \
      postgres -c shared_buffers=2GB \
              -c effective_cache_size=7GB \
              -c work_mem=16MB \
              -c maintenance_work_mem=512MB \
              -c max_connections=100 \
              -c checkpoint_completion_target=0.7 \
              -c max_wal_size=1GB \
              -c min_wal_size=80MB
      
    echo "⏳ Aguardando banco de dados inicializar..."
    sleep 15
fi

# ============================================
# POSTGRES EXPORTER
# ============================================
if docker ps --format '{{.Names}}' | grep -q '^postgres-exporter-prod$'; then
    echo "✅ Postgres Exporter já está rodando"
else
    echo "🔄 Iniciando Postgres Exporter..."
    docker rm -f postgres-exporter-prod 2>/dev/null || true
    docker run -d \
      --name postgres-exporter-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      -e DATA_SOURCE_NAME="postgresql://${DB_USERNAME}:${DB_PASSWORD}@postgres-prod:5432/${DB_NAME}?sslmode=disable" \
      prometheuscommunity/postgres-exporter:v0.15.0
    echo "✅ Postgres Exporter iniciado"
fi

# ============================================
# REDIS
# ============================================
_start_redis() {
    if ! docker volume ls -q -f name=redis-prod-data | grep -q .; then
        docker volume create redis-prod-data
    fi
    docker run -d \
      --name redis-prod \
      --restart unless-stopped \
      --network geosegbar-network \
      redis:7-alpine \
      redis-server --save "" --appendonly no --maxmemory 512mb --maxmemory-policy allkeys-lru
    sleep 5
}

REDIS_RUNNING=$(docker ps --format '{{.Names}}' | grep -q '^redis-prod$' && echo "yes" || echo "no")
REDIS_EXISTS=$(docker ps -a --format '{{.Names}}' | grep -q '^redis-prod$' && echo "yes" || echo "no")

if [ "$REDIS_RUNNING" = "yes" ]; then
    echo "✅ Redis já está rodando"
elif [ "$REDIS_EXISTS" = "yes" ]; then
    echo "🔄 Container do Redis existe mas está parado. Reiniciando..."
    docker network connect geosegbar-network redis-prod 2>/dev/null || true
    docker start redis-prod
    sleep 5
    echo "✅ Redis reiniciado"
else
    echo "📦 Container do Redis não encontrado. Criando..."
    _start_redis
    echo "✅ Redis iniciado"
fi

# Testa conectividade real do Redis na geosegbar-network
echo "🔍 Verificando conectividade do Redis na geosegbar-network..."
if docker run --rm --network geosegbar-network redis:7-alpine \
    redis-cli -h redis-prod ping 2>/dev/null | grep -q "PONG"; then
    echo "✅ Redis acessível"
else
    echo "⚠️ Redis não respondeu ao ping. Forçando recriação na rede correta..."
    docker rm -f redis-prod 2>/dev/null || true
    _start_redis
    if docker run --rm --network geosegbar-network redis:7-alpine \
        redis-cli -h redis-prod ping 2>/dev/null | grep -q "PONG"; then
        echo "✅ Redis acessível após recriação"
    else
        echo "❌ ERRO: Redis não está acessível em geosegbar-network!"
        echo "💡 Debug: docker network inspect geosegbar-network"
        exit 1
    fi
fi

# ============================================
# LIMPEZA DE IMAGENS ANTIGAS (antes do build)
# ============================================
echo "🧹 Limpando imagens e cache Docker não utilizados..."
docker image prune -af 2>/dev/null || true
docker builder prune -af 2>/dev/null || true

# ============================================
# APPLICATION
# ============================================
if [ "$DEPLOY_MODE" = "DB_ONLY" ]; then
  echo "✅ Modo DB_ONLY finalizado (PostgreSQL e Redis preparados)."
  exit 0
fi

# ============================================
# BACKUP PRÉ-DEPLOY
# ============================================
# A aplicação roda migrações de banco (Flyway) no boot. Uma migração falha é
# desfeita pelo Postgres, mas uma migração que aplica algo indesejado só se
# reverte por restore — então o backup acontece ANTES de a API ser derrubada,
# para que uma falha aqui não gere indisponibilidade.
if [ "${SKIP_PRE_DEPLOY_BACKUP:-false}" = "true" ]; then
    echo "⏭️ Pulando backup pré-deploy (SKIP_PRE_DEPLOY_BACKUP=true)"
else
    echo "💾 Backup do banco antes do deploy..."
    if bash "$SCRIPT_DIR/bash/scripts/backup_database_prod.sh"; then
        echo "✅ Backup concluído. Prosseguindo com o deploy."
    else
        echo "❌ Backup pré-deploy FALHOU. Deploy abortado — a API continua no ar."
        echo "💡 Para prosseguir mesmo assim: SKIP_PRE_DEPLOY_BACKUP=true"
        exit 1
    fi
fi

# ============================================
# BLUE-GREEN — troca de versão sem tirar a API do ar
# ============================================
#
# A versão antiga continua atendendo durante TODO o build e o boot da nova. O
# tráfego só migra depois que a nova responde saudável, e a migração é um
# `nginx -s reload` — gracioso, sem derrubar conexão em andamento.
#
# Qualquer falha antes da troca deixa a versão antiga servindo, intacta.
#
# Antes daqui o deploy fazia stop + rm ANTES do build: a API ficava fora do ar
# durante todo o build (minutos) e, se o build ou o boot falhasse, não havia
# para onde voltar — o container antigo já tinha sido removido.

BLUE_CONTAINER="geosegbar-api-prod-blue"
GREEN_CONTAINER="geosegbar-api-prod-green"
LEGACY_CONTAINER="geosegbar-api-prod"

# Estado de runtime, deliberadamente FORA do git: o deploy reescreve este arquivo
# a cada publicação, e um arquivo versionado sendo reescrito faria o `git pull` do
# deploy seguinte falhar por conflito.
RUNTIME_DIR="$SCRIPT_DIR/runtime"
UPSTREAM_FILE="$RUNTIME_DIR/upstream_active.conf"

container_running() {
  [ -n "$(docker ps -q -f "name=^${1}$")" ]
}

container_exists() {
  [ -n "$(docker ps -aq -f "name=^${1}$")" ]
}

# Descobre qual cor está no ar agora. O container legado (nome sem cor) é tratado
# como "azul" para que o primeiro deploy blue-green já saia do jeito certo.
if container_running "$GREEN_CONTAINER"; then
  ACTIVE_CONTAINER="$GREEN_CONTAINER"; IDLE_CONTAINER="$BLUE_CONTAINER";  ACTIVE_COLOR="green"
elif container_running "$BLUE_CONTAINER"; then
  ACTIVE_CONTAINER="$BLUE_CONTAINER";  IDLE_CONTAINER="$GREEN_CONTAINER"; ACTIVE_COLOR="blue"
elif container_running "$LEGACY_CONTAINER"; then
  ACTIVE_CONTAINER="$LEGACY_CONTAINER"; IDLE_CONTAINER="$GREEN_CONTAINER"; ACTIVE_COLOR="legado"
else
  ACTIVE_CONTAINER=""; IDLE_CONTAINER="$BLUE_CONTAINER"; ACTIVE_COLOR="nenhuma"
fi

mkdir -p "$RUNTIME_DIR"

echo "🎨 Versão no ar: ${ACTIVE_COLOR} (${ACTIVE_CONTAINER:-nenhum container ativo})"
echo "🎨 Nova versão subirá como: ${IDLE_CONTAINER}"

if [ "$SKIP_GIT_PULL" = "true" ]; then
  echo "⏭️ Pulando git pull (SKIP_GIT_PULL=true)"
else
  echo "📥 Atualizando código..."
  git pull origin main
fi

GIT_SHA="$(git rev-parse --short HEAD 2>/dev/null || echo manual)"
NEW_IMAGE="geosegbar-prod:${GIT_SHA}"

# ============================================
# BUILD — a versão antiga segue atendendo
# ============================================
echo "🔨 Construindo imagem ${NEW_IMAGE} (API atual continua no ar)..."
if ! docker build -t "$NEW_IMAGE" .; then
  echo "❌ Build FALHOU. Nada foi alterado — a versão anterior continua no ar."
  exit 1
fi
echo "✅ Imagem construída: ${NEW_IMAGE}"

# ============================================
# SOBE A NOVA VERSÃO EM PARALELO
# ============================================
start_api_container() {
  local nome="$1"
  local imagem="$2"

  docker rm -f "$nome" >/dev/null 2>&1 || true

  # O alias fixo mantém o Prometheus funcionando sem saber de cor nenhuma: ele
  # scrapeia geosegbar-api-prod:9090 e o DNS do Docker resolve para quem estiver
  # com o alias. Durante a janela de sobreposição há duas instâncias saudáveis da
  # mesma aplicação, o que é inofensivo para métrica.
  docker run -d \
    --name "$nome" \
    --restart unless-stopped \
    --network geosegbar-network \
    --network-alias geosegbar-api-prod \
    --memory=2g \
    --cpus="1.5" \
    --expose 9090 \
    -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
    -e JAVA_OPTS="${JAVA_OPTS}" \
    -e FLYWAY_ENABLED="${FLYWAY_ENABLED:-true}" \
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
    -e FILE_BASE_URL="${FILE_BASE_URL}" \
    -e FRONTEND_URL="${FRONTEND_URL}" \
    -e SYSTEM_USER_PASSWORD="${SYSTEM_USER_PASSWORD}" \
    -e SOMOSDEVS_USER1_EMAIL="${SOMOSDEVS_USER1_EMAIL}" \
    -e SOMOSDEVS_USER2_EMAIL="${SOMOSDEVS_USER2_EMAIL}" \
    -e SOMOSDEVS_USER3_EMAIL="${SOMOSDEVS_USER3_EMAIL}" \
    -e ADMIN_BYPASS_KEY="${ADMIN_BYPASS_KEY}" \
    -e ANA_API_IDENTIFIER="${ANA_API_IDENTIFIER}" \
    -e ANA_API_PASSWORD="${ANA_API_PASSWORD}" \
    -e ANA_API_AUTH_URL="${ANA_API_AUTH_URL}" \
    -e ANA_API_TELEMETRY_URL="${ANA_API_TELEMETRY_URL}" \
    -e REDIS_HOST="${REDIS_HOST}" \
    -e REDIS_PORT="${REDIS_PORT}" \
    -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
    -e RATE_LIMIT_ENABLED="${RATE_LIMIT_ENABLED}" \
    -e RATE_LIMIT_PUBLIC_CAPACITY="${RATE_LIMIT_PUBLIC_CAPACITY}" \
    -e RATE_LIMIT_PUBLIC_REFILL_TOKENS="${RATE_LIMIT_PUBLIC_REFILL_TOKENS}" \
    -e RATE_LIMIT_PUBLIC_REFILL_DURATION="${RATE_LIMIT_PUBLIC_REFILL_DURATION}" \
    -e RATE_LIMIT_AUTH_CAPACITY="${RATE_LIMIT_AUTH_CAPACITY}" \
    -e RATE_LIMIT_AUTH_REFILL_TOKENS="${RATE_LIMIT_AUTH_REFILL_TOKENS}" \
    -e RATE_LIMIT_AUTH_REFILL_DURATION="${RATE_LIMIT_AUTH_REFILL_DURATION}" \
    -e TZ="${TZ}" \
    -v $SCRIPT_DIR/logs:/app/logs \
    "$imagem"
}

echo "🚀 Subindo nova versão em ${IDLE_CONTAINER}..."
if ! start_api_container "$IDLE_CONTAINER" "$NEW_IMAGE"; then
  echo "❌ Não foi possível iniciar o novo container. Versão anterior segue no ar."
  docker rm -f "$IDLE_CONTAINER" >/dev/null 2>&1 || true
  exit 1
fi

# ============================================
# ESPERA A NOVA VERSÃO FICAR SAUDÁVEL
# ============================================
#
# Consulta a sonda de readiness de dentro do próprio container. Antes o deploy
# fazia `sleep 30` e depois procurava "Started GeosegbarApplication" no log —
# se o boot demorasse mais, o nginx passava a apontar para um container morto.
HEALTH_TIMEOUT_SECONDS="${DEPLOY_HEALTH_TIMEOUT_SECONDS:-300}"
HEALTH_CHECK_INTERVAL_SECONDS="${DEPLOY_HEALTH_CHECK_INTERVAL_SECONDS:-5}"
HEALTH_ELAPSED=0
NEW_HEALTHY=false

echo "⏳ Aguardando ${IDLE_CONTAINER} ficar saudável (timeout ${HEALTH_TIMEOUT_SECONDS}s)..."

while [ "$HEALTH_ELAPSED" -lt "$HEALTH_TIMEOUT_SECONDS" ]; do
  # Se o container morreu (ex.: migração falhou), não adianta seguir esperando.
  if ! container_running "$IDLE_CONTAINER"; then
    echo "❌ O container da nova versão parou sozinho durante o boot."
    break
  fi

  if docker exec "$IDLE_CONTAINER" \
       wget -q --spider -T 5 http://localhost:9090/actuator/health/readiness 2>/dev/null; then
    NEW_HEALTHY=true
    break
  fi

  echo "   ... ainda inicializando (${HEALTH_ELAPSED}s/${HEALTH_TIMEOUT_SECONDS}s)"
  sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
  HEALTH_ELAPSED=$((HEALTH_ELAPSED + HEALTH_CHECK_INTERVAL_SECONDS))
done

if [ "$NEW_HEALTHY" != true ]; then
  echo ""
  echo "❌ A nova versão NÃO ficou saudável. Abortando o deploy."
  echo "🛡️  A versão anterior (${ACTIVE_CONTAINER:-nenhuma}) continua no ar e SEM alteração."
  echo ""

  if docker logs "$IDLE_CONTAINER" 2>&1 | grep -qiE "flyway|Migration.*failed"; then
    echo "🗄️  MIGRAÇÃO DE BANCO — linhas relevantes:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    docker logs "$IDLE_CONTAINER" 2>&1 | grep -iE "flyway|migration|V[0-9]+__" | tail -30
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "💡 Uma migração que falha é desfeita inteira pelo Postgres."
    echo "   O backup pré-deploy também está disponível."
    echo ""
  fi

  echo "📋 Últimas linhas do log da versão que falhou:"
  docker logs --tail 60 "$IDLE_CONTAINER" 2>&1 || true

  docker rm -f "$IDLE_CONTAINER" >/dev/null 2>&1 || true
  exit 1
fi

echo "✅ ${IDLE_CONTAINER} está saudável."

# ============================================
# NGINX — sobe só se ainda não existir
# ============================================
#
# O nginx NÃO é recriado a cada deploy. Ele é o que mantém a porta pública de pé:
# recriá-lo abria uma janela de indisponibilidade a cada publicação, sem motivo.
# A troca de versão é feita por reload de configuração, mais abaixo.
write_upstream() {
  mkdir -p "$RUNTIME_DIR"
  echo "set \$upstream_server ${1}:9090;" > "$UPSTREAM_FILE"
}

if ! container_running nginx-prod; then
  echo "🚀 Nginx não estava no ar — subindo..."
  docker rm -f nginx-prod >/dev/null 2>&1 || true

  # Aponta direto para a nova versão, já saudável.
  write_upstream "$IDLE_CONTAINER"

  docker run -d \
    --name nginx-prod \
    --restart unless-stopped \
    --network geosegbar-network \
    -p ${SERVER_PORT}:80 \
    -v $SCRIPT_DIR/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro \
    -v $UPSTREAM_FILE:/etc/nginx/conf.d/upstream_active.conf:ro \
    nginx:alpine

  sleep 5
  echo "✅ Nginx iniciado apontando para ${IDLE_CONTAINER}"
  UPSTREAM_JA_TROCADO=true
else
  echo "✅ Nginx já está no ar — será apenas recarregado (sem downtime)"
  UPSTREAM_JA_TROCADO=false
fi

# ============================================
# TROCA DE TRÁFEGO
# ============================================
PREVIOUS_UPSTREAM=""
if [ -f "$UPSTREAM_FILE" ]; then
  PREVIOUS_UPSTREAM="$(cat "$UPSTREAM_FILE")"
fi

if [ "$UPSTREAM_JA_TROCADO" != true ]; then
  echo "🔀 Direcionando o tráfego para ${IDLE_CONTAINER}..."
  write_upstream "$IDLE_CONTAINER"

  if ! docker exec nginx-prod nginx -t >/dev/null 2>&1; then
    echo "❌ Configuração do nginx inválida. Revertendo e mantendo a versão anterior."
    [ -n "$PREVIOUS_UPSTREAM" ] && echo "$PREVIOUS_UPSTREAM" > "$UPSTREAM_FILE"
    docker rm -f "$IDLE_CONTAINER" >/dev/null 2>&1 || true
    exit 1
  fi

  docker exec nginx-prod nginx -s reload
  echo "✅ Tráfego migrado (reload gracioso, nenhuma conexão derrubada)"
fi

# ============================================
# SMOKE TEST — pela porta pública, como um usuário real
# ============================================
echo "🔬 Validando a nova versão pela porta pública..."
SMOKE_OK=false
for tentativa in 1 2 3 4 5; do
  CODIGO="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
            "http://localhost:${SERVER_PORT}/actuator/health/readiness" 2>/dev/null || echo 000)"
  # 404 é esperado e correto: o nginx bloqueia /actuator/ vindo de fora. O que
  # importa é que a requisição ATRAVESSOU o nginx — 502/000 significaria upstream
  # morto. Por isso o teste também bate numa rota real da aplicação.
  CODIGO_APP="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
                "http://localhost:${SERVER_PORT}/user/login/initiate" -X POST \
                -H 'Content-Type: application/json' -d '{}' 2>/dev/null || echo 000)"

  if [ "$CODIGO_APP" != "000" ] && [ "$CODIGO_APP" != "502" ] && [ "$CODIGO_APP" != "504" ]; then
    SMOKE_OK=true
    break
  fi
  echo "   tentativa ${tentativa}/5 — resposta ${CODIGO_APP}, aguardando..."
  sleep 3
done

if [ "$SMOKE_OK" != true ]; then
  echo ""
  echo "❌ A nova versão não respondeu pela porta pública. REVERTENDO."

  if [ -n "$PREVIOUS_UPSTREAM" ] && [ -n "$ACTIVE_CONTAINER" ] && container_running "$ACTIVE_CONTAINER"; then
    echo "$PREVIOUS_UPSTREAM" > "$UPSTREAM_FILE"
    docker exec nginx-prod nginx -s reload
    echo "✅ Tráfego devolvido para ${ACTIVE_CONTAINER}. Versão anterior no ar."
  else
    echo "⚠️  Não havia versão anterior no ar para reverter."
  fi

  docker logs --tail 60 "$IDLE_CONTAINER" 2>&1 || true
  docker rm -f "$IDLE_CONTAINER" >/dev/null 2>&1 || true
  exit 1
fi

echo "✅ Smoke test aprovado."

# ============================================
# ENCERRA A VERSÃO ANTIGA
# ============================================
if [ -n "$ACTIVE_CONTAINER" ] && container_exists "$ACTIVE_CONTAINER"; then
  DRAIN_SECONDS="${DEPLOY_DRAIN_SECONDS:-15}"
  echo "🚪 Aguardando ${DRAIN_SECONDS}s para as requisições em andamento na versão anterior terminarem..."
  sleep "$DRAIN_SECONDS"

  echo "🛑 Encerrando ${ACTIVE_CONTAINER}..."
  docker stop "$ACTIVE_CONTAINER" >/dev/null 2>&1 || true
  docker rm "$ACTIVE_CONTAINER" >/dev/null 2>&1 || true
fi

# Guarda a imagem anterior com tag fixa, para rollback manual em um comando.
if docker image inspect geosegbar-prod:latest >/dev/null 2>&1; then
  docker tag geosegbar-prod:latest geosegbar-prod:previous >/dev/null 2>&1 || true
fi
docker tag "$NEW_IMAGE" geosegbar-prod:latest

echo "🏷️  Imagem publicada: ${NEW_IMAGE} (também marcada como :latest)"
echo "🏷️  Versão anterior preservada como geosegbar-prod:previous"

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
# A saúde da nova versão já foi validada antes da troca de tráfego, e um smoke
# test pela porta pública confirmou o caminho ponta a ponta. Se chegou aqui, o
# deploy deu certo — resta reportar o estado.
echo ""
echo "✅ Deploy em PRODUÇÃO concluído SEM indisponibilidade!"
echo ""
echo "📡 SERVIÇOS DISPONÍVEIS:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 API (nginx):   http://localhost:${SERVER_PORT}"
echo "🎨 Versão no ar:  ${IDLE_CONTAINER}  (imagem ${NEW_IMAGE})"
echo "📊 Prometheus:    http://localhost:9091"
echo "📈 Grafana:       http://localhost:3001 (admin / ${GRAFANA_PASSWORD})"
echo "🗄️  PostgreSQL:    localhost:${DB_PORT}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "↩️  Para voltar à versão anterior:  ./bash/cli_app.sh  →  Rollback produção"
echo ""
echo "📊 Status dos containers:"
docker ps --filter "name=geosegbar" --filter "name=postgres-prod" --filter "name=nginx-prod" --filter "name=prometheus-prod" --filter "name=grafana-prod"

echo ""
echo "🧹 Limpando imagens órfãs (a :previous é preservada)..."
docker image prune -f > /dev/null 2>&1 || true

echo ""
echo "🎉 Deploy em PRODUÇÃO concluído com monitoramento completo!"