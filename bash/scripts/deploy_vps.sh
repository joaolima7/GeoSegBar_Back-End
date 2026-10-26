#!/bin/bash

set -e

if [ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

echo "🚀 Iniciando deploy da API GeoSegBar em PRODUÇÃO..."

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
    set +e
    bash "$SCRIPT_DIR/bash/scripts/backup_database_prod.sh"
    BACKUP_EXIT=$?
    set -e

    case "$BACKUP_EXIT" in
      0)
        echo "✅ Backup concluído (local + S3). Prosseguindo com o deploy."
        ;;
      2)
        # Dump local íntegro, cópia no S3 ausente. O ponto de restauração que o
        # deploy precisa existe — travar a publicação aqui trocaria um problema
        # de durabilidade por indisponibilidade. Segue, mas sem disfarçar.
        echo ""
        echo "⚠️  ATENÇÃO: o backup local foi criado e verificado, mas NÃO subiu para o S3."
        echo "   Existe apenas UMA cópia, no próprio servidor."
        echo "   O deploy prossegue — o ponto de restauração existe —, mas resolva o S3."
        echo "   Detalhes do erro: $SCRIPT_DIR/logs/backup.log"
        echo ""
        ;;
      *)
        echo "❌ Backup pré-deploy FALHOU: nenhum dump foi gerado."
        echo "   Deploy abortado — a versão atual continua no ar, sem alteração."
        echo "   Detalhes: $SCRIPT_DIR/logs/backup.log"
        echo "💡 Para prosseguir mesmo assim: SKIP_PRE_DEPLOY_BACKUP=true"
        exit 1
        ;;
    esac
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
  SHA_ANTES_DO_PULL="$(git rev-parse HEAD 2>/dev/null || echo '')"
  git pull origin main

  # O bash continua executando o corpo ANTIGO deste arquivo depois do pull (ele
  # já está aberto; o git grava um arquivo novo no lugar). Se o pull mudou este
  # script, reexecuta a versão nova a partir daqui: Postgres e Redis são
  # conferidos de novo (idempotente) e o backup já foi feito.
  if [ -n "$SHA_ANTES_DO_PULL" ] && \
     ! git diff --quiet "$SHA_ANTES_DO_PULL" HEAD -- bash/scripts/deploy_vps.sh; then
    echo "🔁 O deploy_vps.sh mudou no pull. Reexecutando a versão nova..."
    exec env SKIP_GIT_PULL=true SKIP_PRE_DEPLOY_BACKUP=true bash "$SCRIPT_DIR/bash/scripts/deploy_vps.sh"
  fi
fi

# O Prometheus legado (antes de monitoring/) coletava geosegbar-api-prod:9090
# /actuator/prometheus. O actuator agora mora na 8081: na 9090 o caminho dá 404
# e o container legado só gera ruído. O volume dele fica; o monitoramento novo
# tem deploy próprio (docs/MONITORAMENTO.md).
if docker ps -aq -f 'name=^prometheus-prod$' | grep -q .; then
  docker rm -f prometheus-prod >/dev/null 2>&1 || true
  echo "🧹 Prometheus legado (prometheus-prod) removido; volume mantido."
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

  # O alias fixo é o nome estável da API dentro da rede do Docker.
  #
  # O monitoramento (monitoring/, deploy separado) acha cada container pelo label
  # com.geosegbar.scrape e coleta na porta de management 8081: blue e green viram
  # instâncias distintas nos painéis, inclusive na janela de sobreposição.
  #
  # Log do Docker com rotação: sem max-size o json-file cresce sem limite e o
  # disco enche. 5 x 50 MB por container.
  docker run -d \
    --name "$nome" \
    --restart unless-stopped \
    --network geosegbar-network \
    --network-alias geosegbar-api-prod \
    --memory=2g \
    --cpus="1.5" \
    --expose 9090 \
    --expose 8081 \
    --label com.geosegbar.scrape=true \
    --label com.geosegbar.service=api \
    --label "com.geosegbar.version=${GIT_SHA}" \
    --log-driver json-file \
    --log-opt max-size=50m \
    --log-opt max-file=5 \
    -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
    -e APP_VERSION="${GIT_SHA}" \
    -e TRACING_ENABLED="${TRACING_ENABLED_EFETIVO}" \
    -e TRACING_SAMPLING_PROBABILITY="${TRACING_SAMPLING_PROBABILITY:-1.0}" \
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

# Tracing: liga quando o coletor (geosegbar-alloy, de monitoring/) está no ar.
# Sem ele o exportador OTLP só acumularia falhas de envio no log. TRACING_ENABLED
# no .env.prod, se definido, vence a detecção.
if [ -n "${TRACING_ENABLED:-}" ]; then
  TRACING_ENABLED_EFETIVO="$TRACING_ENABLED"
elif [ -n "$(docker ps -q -f 'name=^geosegbar-alloy$')" ]; then
  TRACING_ENABLED_EFETIVO=true
else
  TRACING_ENABLED_EFETIVO=false
fi
echo "🔭 Tracing: ${TRACING_ENABLED_EFETIVO}"

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

  # -O /dev/null em vez de --spider: --spider faz HEAD, e HEAD é justamente o que
  # tropeçava na regra de segurança. GET é o que o endpoint realmente serve.
  if docker exec "$IDLE_CONTAINER" \
       wget -q -O /dev/null -T 5 http://localhost:8081/actuator/health/readiness 2>/dev/null; then
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
    -v $UPSTREAM_FILE:/etc/nginx/upstream_active.conf:ro \
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

  # Regenera a configuração a partir do template versionado.
  #
  # Sem isto, mudança em nginx/default.conf.template NUNCA chega em produção:
  # quem transforma template em configuração é o entrypoint da imagem, e ele só
  # roda quando o container SOBE. Como o nginx não é recriado a cada deploy (de
  # propósito, para não abrir janela de indisponibilidade), o `nginx -s reload`
  # relia o /etc/nginx/conf.d/default.conf antigo e a mudança passava
  # despercebida — deploy verde, configuração velha. Foi o que aconteceu com os
  # ajustes de timeout: o container subiu em 26/08 e seguiu com os valores
  # daquele dia.
  #
  # O template vai pela entrada padrão, lido do host. Ler o arquivo montado em
  # /etc/nginx/templates/ não serve: o `git pull` troca o arquivo por outro
  # (inode novo) e o bind mount de arquivo único continua apontando para o
  # antigo até o container ser recriado.
  #
  # Dentro do sh -c, "$UPSTREAM_SERVER" é expandido pelo shell do container, onde
  # a variável não existe: o comando vira `envsubst ""`, que não substitui nada.
  # É o que precisa: as variáveis do próprio nginx ($host, $remote_addr,
  # $request_id) passam intactas, e o template não usa ${VAR} de ambiente. Se um
  # dia usar, troque por envsubst '${NOME}' com a lista explícita.
  docker exec nginx-prod cp /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak
  docker exec -i nginx-prod sh -c \
    'envsubst "$UPSTREAM_SERVER" > /etc/nginx/conf.d/default.conf' < "$SCRIPT_DIR/nginx/default.conf.template"

  if ! docker exec nginx-prod nginx -t >/dev/null 2>&1; then
    echo "❌ Configuração do nginx inválida. Revertendo e mantendo a versão anterior."
    docker exec nginx-prod cp /etc/nginx/conf.d/default.conf.bak /etc/nginx/conf.d/default.conf
    [ -n "$PREVIOUS_UPSTREAM" ] && echo "$PREVIOUS_UPSTREAM" > "$UPSTREAM_FILE"
    docker rm -f "$IDLE_CONTAINER" >/dev/null 2>&1 || true
    exit 1
  fi

  docker exec nginx-prod nginx -s reload
  echo "✅ Tráfego migrado e configuração do nginx sincronizada (reload gracioso)"
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
echo "📈 Monitoramento: Grafana pela tailnet ou túnel SSH (docs/MONITORAMENTO.md)"
echo "🗄️  PostgreSQL:    localhost:${DB_PORT}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "↩️  Para voltar à versão anterior:  ./bash/cli_app.sh  →  Rollback produção"
echo ""
echo "📊 Status dos containers:"
docker ps --filter "name=geosegbar-api" --filter "name=postgres-prod" --filter "name=redis-prod" --filter "name=nginx-prod"

echo ""
echo "🧹 Limpando imagens órfãs (a :previous é preservada)..."
docker image prune -f > /dev/null 2>&1 || true

echo ""
echo "🎉 Deploy em PRODUÇÃO concluído!"