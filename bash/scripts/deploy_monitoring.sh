#!/usr/bin/env bash
#
# Deploy da stack de monitoramento (monitoring/) em produção.
#
# Independente do deploy da API: um não reinicia o outro. Ordem:
#
#   1. Pré-requisitos (root, Docker, compose, rede geosegbar-network).
#   2. Primeira execução: cria /etc/geosegbar/monitoring (config e segredos),
#      gera as senhas internas e PARA, pedindo os segredos externos (Telegram,
#      healthchecks.io). Nada sobe sem eles.
#   3. Monta uma cópia de monitoring/ em área de preparo e valida cada arquivo
#      com o binário da própria imagem (promtool, amtool, loki, alloy, compose).
#      Qualquer erro aborta aqui, com a stack antiga intacta.
#   4. Troca /opt/geosegbar-monitoring pela cópia validada (dono root, fora da
#      home do wwvpsb) e recria os containers.
#   5. Confere saúde, alvos de coleta e portas publicadas; remove os containers
#      legados prometheus-prod, grafana-prod e postgres-exporter-prod (volumes
#      ficam).
#
# Uso: ./bash/cli_app.sh → "Deploy do monitoramento". Detalhes em
# docs/MONITORAMENTO.md.

set -Eeuo pipefail

if [ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC_DIR="$ROOT_DIR/monitoring"

CONFIG_DIR="/etc/geosegbar/monitoring"
ENV_FILE="$CONFIG_DIR/monitoring.env"
SECRETS_DIR="$CONFIG_DIR/secrets"
INSTALL_DIR="/opt/geosegbar-monitoring"
STAGE_DIR="${INSTALL_DIR}.novo"
PREV_DIR="${INSTALL_DIR}.anterior"
TEXTFILE_DIR="/var/lib/geosegbar-monitoring/textfile"
PROJECT="geosegbar-monitoring"

# Segredos gerados aqui (internos) e segredos que só o João tem (externos).
GENERATED_SECRETS=(grafana_admin_password pg_monitoring_password)
EXTERNAL_SECRETS=(telegram_bot_token telegram_chat_id healthchecks_watchdog_url)

ok()   { echo "✅ $*"; }
info() { echo "ℹ️  $*"; }
warn() { echo "⚠️  $*"; }
die()  { echo "❌ $*" >&2; exit 1; }

# ---------------------------------------------------------------- pré-requisitos
[ "$(id -u)" -eq 0 ] || die "Rode como root: o script grava em /etc, /opt e /var/lib."
docker info >/dev/null 2>&1 || die "Docker não está rodando."
docker compose version >/dev/null 2>&1 \
  || die "Plugin docker compose ausente. Instale: dnf install -y docker-compose-plugin"
docker network inspect geosegbar-network >/dev/null 2>&1 \
  || die "Rede geosegbar-network não existe. Rode o deploy da API primeiro."
command -v openssl >/dev/null 2>&1 || die "openssl ausente (usado para gerar as senhas)."
[ -f "$SRC_DIR/docker-compose.yml" ] || die "monitoring/docker-compose.yml não encontrado em $SRC_DIR"

compose() {
  docker compose -p "$PROJECT" --env-file "$ENV_FILE" -f "$1/docker-compose.yml" "${@:2}"
}

# Imagem de cada ferramenta de validação: a mesma tag+digest do compose.
image_of() {
  local servico="$1"
  awk -v s="  ${servico}:" '
    $0 == s { dentro = 1; next }
    dentro && /^  [a-z]/ { exit }
    dentro && /^    image:/ { print $2; exit }
  ' "$SRC_DIR/docker-compose.yml"
}

# ------------------------------------------------------- configuração e segredos
mkdir -p "$CONFIG_DIR" "$SECRETS_DIR"
chmod 700 "$CONFIG_DIR" "$SECRETS_DIR"
chown root:root "$CONFIG_DIR" "$SECRETS_DIR"

if [ ! -f "$ENV_FILE" ]; then
  cp "$SRC_DIR/.env.example" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  # O banco observado é o da API.
  if [ -n "${DB_NAME:-}" ]; then
    sed -i "s|^PG_EXPORTER_DATABASE=.*|PG_EXPORTER_DATABASE=${DB_NAME}|" "$ENV_FILE"
  fi
  info "Criado $ENV_FILE a partir de monitoring/.env.example."
fi

# Segredo em arquivo 0444 dentro de pasta 0700 de root: no host só root lê; no
# container, o compose monta o arquivo isolado e o processo (não-root) consegue
# ler. Sem quebra de linha no fim.
for nome in "${GENERATED_SECRETS[@]}"; do
  arquivo="$SECRETS_DIR/$nome"
  if [ ! -s "$arquivo" ]; then
    openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 40 > "$arquivo"
    chmod 444 "$arquivo"
    info "Gerado $arquivo"
  fi
done

faltando=()
for nome in "${EXTERNAL_SECRETS[@]}"; do
  arquivo="$SECRETS_DIR/$nome"
  if [ ! -s "$arquivo" ]; then
    : > "$arquivo"
    chmod 444 "$arquivo"
    faltando+=("$nome")
  fi
done

if [ "${#faltando[@]}" -gt 0 ]; then
  echo ""
  warn "Faltam segredos externos. Preencha cada arquivo (sem quebra de linha no fim):"
  for nome in "${faltando[@]}"; do
    echo "     $SECRETS_DIR/$nome"
  done
  echo ""
  echo "   Exemplo:  printf '%s' 'VALOR' > $SECRETS_DIR/telegram_bot_token"
  echo "   Como obter cada um: docs/MONITORAMENTO.md, seção \"Primeira instalação\"."
  echo "   Revise também $ENV_FILE (GRAFANA_ROOT_URL, PG_EXPORTER_DATABASE, BLACKBOX_TARGETS)."
  echo "   Depois rode este deploy de novo."
  exit 3
fi

# Arquivos que alguém editou à mão podem ter ficado graváveis ou com \n no fim.
for nome in "${GENERATED_SECRETS[@]}" "${EXTERNAL_SECRETS[@]}"; do
  arquivo="$SECRETS_DIR/$nome"
  if [ "$(tail -c 1 "$arquivo" | od -An -c | tr -d ' ')" = '\n' ]; then
    printf '%s' "$(cat "$arquivo")" > "$arquivo.tmp" && mv -f "$arquivo.tmp" "$arquivo"
  fi
  chown root:root "$arquivo"
  chmod 444 "$arquivo"
done

grep -Eq '^[-0-9]+$' "$SECRETS_DIR/telegram_chat_id" \
  || die "telegram_chat_id deve ser o id numérico do chat (ex. -1001234567890)."
grep -Eq '^https://' "$SECRETS_DIR/healthchecks_watchdog_url" \
  || die "healthchecks_watchdog_url deve começar com https://"

# Pasta que o script de backup usa para publicar a métrica de último sucesso.
mkdir -p "$TEXTFILE_DIR"
chmod 755 "$(dirname "$TEXTFILE_DIR")" "$TEXTFILE_DIR"

# IP privado da ponte docker0: o node_exporter escuta só ali.
if ! grep -Eq '^NODE_EXPORTER_LISTEN=.+' "$ENV_FILE"; then
  # O "|| true" é de propósito: sem docker0 o ip falha e, com pipefail, o set -e
  # sairia aqui calado, sem chegar à mensagem abaixo.
  DOCKER0_IP="$( { ip -4 -o addr show docker0 2>/dev/null || true; } | awk '{print $4}' | cut -d/ -f1 | head -1)"
  [ -n "$DOCKER0_IP" ] || die "Não achei o IP da docker0. Defina NODE_EXPORTER_LISTEN em $ENV_FILE."
  case "$DOCKER0_IP" in
    10.*|172.1[6-9].*|172.2[0-9].*|172.3[0-1].*|192.168.*) ;;
    *) die "docker0 com IP não privado ($DOCKER0_IP). Defina NODE_EXPORTER_LISTEN à mão." ;;
  esac
  if grep -q '^NODE_EXPORTER_LISTEN=' "$ENV_FILE"; then
    sed -i "s|^NODE_EXPORTER_LISTEN=.*|NODE_EXPORTER_LISTEN=${DOCKER0_IP}:9100|" "$ENV_FILE"
  else
    echo "NODE_EXPORTER_LISTEN=${DOCKER0_IP}:9100" >> "$ENV_FILE"
  fi
  info "node_exporter vai escutar em ${DOCKER0_IP}:9100"
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

[ "${GSB_SECRETS_DIR:-}" = "$SECRETS_DIR" ] \
  || die "GSB_SECRETS_DIR em $ENV_FILE deve ser $SECRETS_DIR"
case "${NODE_EXPORTER_LISTEN:-}" in
  0.0.0.0:*|:*|"") die "NODE_EXPORTER_LISTEN não pode ser vazio nem 0.0.0.0." ;;
esac

# ---------------------------------------------------------------- preparo
echo ""
echo "📦 Preparando cópia de monitoring/ em $STAGE_DIR..."
rm -rf "$STAGE_DIR"
mkdir -p "$STAGE_DIR"
cp -a "$SRC_DIR/." "$STAGE_DIR/"
# O que é só de desenvolvimento não vai para produção.
rm -rf "$STAGE_DIR/.secrets-dev" "$STAGE_DIR/.env" "$STAGE_DIR/.env.dev" \
       "$STAGE_DIR/docker-compose.dev.yml" "$STAGE_DIR/alertmanager/alertmanager.dev.yml" \
       "$STAGE_DIR/scripts"

# Alvos do blackbox a partir de BLACKBOX_TARGETS.
mkdir -p "$STAGE_DIR/prometheus/targets"
{
  echo "# Gerado por deploy_monitoring.sh a partir de BLACKBOX_TARGETS. Não edite."
  echo "- targets:"
  IFS=',' read -r -a ALVOS <<< "${BLACKBOX_TARGETS:-}"
  for alvo in "${ALVOS[@]}"; do
    alvo="$(echo "$alvo" | xargs)"
    [ -n "$alvo" ] || continue
    case "$alvo" in
      https://*) echo "    - \"$alvo\"" ;;
      *) die "BLACKBOX_TARGETS só aceita URLs https:// ($alvo)" ;;
    esac
  done
  echo "  labels:"
  echo "    probe: public"
} > "$STAGE_DIR/prometheus/targets/blackbox.yml"

chown -R root:root "$STAGE_DIR"
find "$STAGE_DIR" -type d -exec chmod 755 {} +
find "$STAGE_DIR" -type f -exec chmod 644 {} +

# ---------------------------------------------------------------- validação
echo "🔎 Validando configurações com os binários das imagens..."

valida() {
  local titulo="$1"; shift
  local saida
  if ! saida="$("$@" 2>&1)"; then
    echo "$saida" | tail -30
    rm -rf "$STAGE_DIR"
    die "Configuração inválida: $titulo. Nada foi trocado; a stack atual segue no ar."
  fi
  ok "$titulo"
}

PROM_IMAGE="$(image_of prometheus)"
AM_IMAGE="$(image_of alertmanager)"
LOKI_IMAGE="$(image_of loki)"
TEMPO_IMAGE="$(image_of tempo)"
ALLOY_IMAGE="$(image_of alloy)"
BLACKBOX_IMAGE="$(image_of blackbox)"
for img in "$PROM_IMAGE" "$AM_IMAGE" "$LOKI_IMAGE" "$TEMPO_IMAGE" "$ALLOY_IMAGE" "$BLACKBOX_IMAGE"; do
  [ -n "$img" ] || die "Não consegui ler a imagem de um serviço no docker-compose.yml."
done

valida "docker compose config" compose "$STAGE_DIR" config -q

valida "prometheus.yml (promtool check config)" \
  docker run --rm --network none --entrypoint promtool \
    -v "$STAGE_DIR/prometheus:/etc/prometheus:ro" \
    "$PROM_IMAGE" check config /etc/prometheus/prometheus.yml

valida "regras do Prometheus (promtool check rules)" \
  docker run --rm --network none --entrypoint sh \
    -v "$STAGE_DIR/prometheus:/etc/prometheus:ro" \
    "$PROM_IMAGE" -c 'promtool check rules /etc/prometheus/rules/*.yml'

valida "alertmanager.yml e templates (amtool check-config)" \
  docker run --rm --network none --entrypoint amtool \
    -v "$STAGE_DIR/alertmanager:/etc/alertmanager:ro" \
    "$AM_IMAGE" check-config /etc/alertmanager/alertmanager.yml

valida "loki.yml (loki -verify-config)" \
  docker run --rm --network none \
    -v "$STAGE_DIR/loki:/etc/loki:ro" \
    "$LOKI_IMAGE" -config.file=/etc/loki/loki.yml -verify-config

valida "tempo.yml (tempo -config.verify)" \
  docker run --rm --network none \
    -v "$STAGE_DIR/tempo:/etc/tempo:ro" \
    "$TEMPO_IMAGE" -config.file=/etc/tempo/tempo.yml -config.verify

valida "blackbox.yml (blackbox --config.check)" \
  docker run --rm --network none \
    -v "$STAGE_DIR/blackbox:/etc/blackbox:ro" \
    "$BLACKBOX_IMAGE" --config.file=/etc/blackbox/blackbox.yml --config.check

valida "config.alloy (alloy validate)" \
  docker run --rm --network none \
    -v "$STAGE_DIR/alloy:/etc/alloy:ro" \
    "$ALLOY_IMAGE" validate /etc/alloy/config.alloy

if command -v python3 >/dev/null 2>&1; then
  valida "painéis do Grafana (check_dashboards.py)" \
    python3 -I "$SRC_DIR/grafana/dashboards-src/check_dashboards.py"
else
  warn "python3 ausente: painéis não validados aqui (o repositório valida antes do commit)."
fi

# ---------------------------------------------------------------- troca
echo ""
echo "🔁 Instalando em $INSTALL_DIR..."
rm -rf "$PREV_DIR"
if [ -d "$INSTALL_DIR" ]; then
  mv "$INSTALL_DIR" "$PREV_DIR"
fi
mv "$STAGE_DIR" "$INSTALL_DIR"
chmod 755 "$INSTALL_DIR"

echo "⬇️  Baixando imagens (tag + digest fixos)..."
compose "$INSTALL_DIR" pull --quiet

# --force-recreate: os arquivos de configuração são montados um a um; um
# container antigo continuaria lendo a versão anterior (o mount aponta para o
# arquivo antigo, não para o caminho).
echo "🚀 Subindo a stack..."
compose "$INSTALL_DIR" up -d --force-recreate --remove-orphans

# ---------------------------------------------------------------- saúde
echo "⏳ Aguardando os serviços ficarem saudáveis..."
espera_saudavel() {
  local container="$1" limite="${2:-180}" passado=0 estado
  while [ "$passado" -lt "$limite" ]; do
    # Container que já reiniciou está em loop: pode ser visto "running" entre
    # duas quedas e passaria por saudável.
    rc="$(docker inspect -f '{{.RestartCount}}' "$container" 2>/dev/null || true)"
    if [ -n "$rc" ] && [ "$rc" != "0" ]; then
      echo "❌ $container reiniciou depois de subir (loop de falha)."
      break
    fi
    estado="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || echo ausente)"
    case "$estado" in
      healthy|running) ok "$container: $estado"; return 0 ;;
      exited|dead|ausente) break ;;
    esac
    sleep 5
    passado=$((passado + 5))
  done
  echo "--- últimas linhas de $container ---"
  docker logs --tail 40 "$container" 2>&1 || true
  return 1
}

FALHAS=0
for c in geosegbar-prometheus geosegbar-alertmanager geosegbar-grafana geosegbar-loki; do
  espera_saudavel "$c" 180 || FALHAS=$((FALHAS + 1))
done
# Sem healthcheck próprio: basta estar rodando, sem reinício, depois de um tempo.
sleep 10
for c in geosegbar-tempo geosegbar-alloy geosegbar-node-exporter \
         geosegbar-cadvisor geosegbar-postgres-exporter geosegbar-redis-exporter \
         geosegbar-blackbox geosegbar-socket-proxy; do
  espera_saudavel "$c" 60 || FALHAS=$((FALHAS + 1))
done

if [ "$FALHAS" -gt 0 ]; then
  echo ""
  warn "$FALHAS serviço(s) não ficaram saudáveis. A configuração anterior está em $PREV_DIR."
  warn "Para voltar: mv $INSTALL_DIR ${INSTALL_DIR}.falhou && mv $PREV_DIR $INSTALL_DIR && \\"
  warn "  docker compose -p $PROJECT --env-file $ENV_FILE -f $INSTALL_DIR/docker-compose.yml up -d --force-recreate"
  exit 1
fi

# ---------------------------------------------------------------- conferências
echo ""
echo "🔐 Conferindo portas publicadas..."
PUBLICADAS="$(docker ps --filter "label=com.docker.compose.project=$PROJECT" --format '{{.Names}} {{.Ports}}' \
              | grep -E '[0-9]+->' || true)"
echo "${PUBLICADAS:-   (nenhuma)}"
if echo "$PUBLICADAS" | grep -E '(0\.0\.0\.0|\[::\]|:::)[^ ]*->' >/dev/null; then
  die "Há porta do monitoramento publicada em todas as interfaces. Corrija o docker-compose.yml."
fi
ok "Só o Grafana em 127.0.0.1."

echo "🎯 Alvos de coleta do Prometheus:"
docker exec geosegbar-prometheus wget -qO- "http://localhost:9090/api/v1/targets?state=active" 2>/dev/null \
  | grep -o '"scrapePool":"[^"]*"\|"health":"[a-z]*"' | paste - - | sort | uniq -c || true

if ! docker exec geosegbar-prometheus wget -qO /dev/null -T 5 "http://host.docker.internal:9100/metrics" 2>/dev/null; then
  warn "O Prometheus não alcança o node_exporter em ${NODE_EXPORTER_LISTEN}."
  warn "Se o firewall nftables nega por padrão, libere SÓ a origem das redes do Docker. Use"
  warn "insert (antes da regra final de descarte), não add:"
  warn "  nft insert rule inet geosegbar_fw <cadeia> iifname \"br-*\" ip daddr ${NODE_EXPORTER_LISTEN%%:*} tcp dport 9100 accept"
  warn "Veja docs/MONITORAMENTO.md, \"node_exporter inalcançável\"."
fi

# ---------------------------------------------------------------- legado
for legado in prometheus-prod grafana-prod postgres-exporter-prod; do
  if docker ps -aq -f "name=^${legado}$" | grep -q .; then
    docker rm -f "$legado" >/dev/null
    ok "Container legado $legado removido (o volume dele foi mantido)."
  fi
done

echo ""
ok "Monitoramento no ar."
echo ""
echo "📈 Grafana: ${GRAFANA_ROOT_URL} (usuário ${GRAFANA_ADMIN_USER})"
echo "   Senha inicial do admin: $SECRETS_DIR/grafana_admin_password (só root lê)."
echo "   Sem Tailscale: ssh -N -L 3000:127.0.0.1:3000 <usuário>@<servidor> e abra http://localhost:3000"
API_NO_AR="$(docker ps --format '{{.Names}}' | grep -E '^geosegbar-api-prod-(blue|green)$' | head -1 || true)"
if [ -z "$API_NO_AR" ] || ! docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$API_NO_AR" 2>/dev/null \
     | grep -qx 'TRACING_ENABLED=true'; then
  echo ""
  info "Os traces e a coleta da API começam depois do próximo deploy da API: ele"
  info "detecta o geosegbar-alloy e liga TRACING_ENABLED sozinho."
fi
