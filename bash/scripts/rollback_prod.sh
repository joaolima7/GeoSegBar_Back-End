#!/bin/bash
#
# Rollback de produção — volta para a imagem imediatamente anterior.
#
# Sobe a versão anterior em paralelo, espera ficar saudável e só então migra o
# tráfego. Se a versão anterior não subir, a atual continua no ar: um rollback
# que falha não pode piorar a situação.
#
# Use quando o deploy passou nos testes automáticos mas o problema apareceu
# depois — erro que só se manifesta com tráfego real, regressão funcional, etc.

set -e

if [ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]; then
  echo "❌ Execução direta não permitida. Use: ./bash/cli_app.sh"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f .env.prod ]; then
  echo "❌ Arquivo .env.prod não encontrado em $SCRIPT_DIR"
  exit 1
fi
set -a
# shellcheck disable=SC1091
. ./.env.prod
set +a

BLUE_CONTAINER="geosegbar-api-prod-blue"
GREEN_CONTAINER="geosegbar-api-prod-green"
UPSTREAM_FILE="$SCRIPT_DIR/runtime/upstream_active.conf"

container_running() {
  [ -n "$(docker ps -q -f "name=^${1}$")" ]
}

echo "↩️  Rollback de produção"
echo ""

# ---------------------------------------------------------------- pré-checagem
if ! docker image inspect geosegbar-prod:previous >/dev/null 2>&1; then
  echo "❌ Não existe imagem geosegbar-prod:previous."
  echo "   O rollback automático só funciona a partir do segundo deploy blue-green."
  echo ""
  echo "   Imagens disponíveis:"
  docker images geosegbar-prod --format '   {{.Tag}}  ({{.CreatedSince}})'
  echo ""
  echo "   Para voltar a uma tag específica, use o deploy com a imagem desejada."
  exit 1
fi

if ! container_running nginx-prod; then
  echo "❌ O nginx não está no ar — não há para onde direcionar tráfego."
  echo "   Rode um deploy normal: ./bash/cli_app.sh → Deploy produção"
  exit 1
fi

if container_running "$GREEN_CONTAINER"; then
  ATUAL="$GREEN_CONTAINER"; DESTINO="$BLUE_CONTAINER"
elif container_running "$BLUE_CONTAINER"; then
  ATUAL="$BLUE_CONTAINER"; DESTINO="$GREEN_CONTAINER"
else
  echo "❌ Nenhum container blue/green no ar. Rode um deploy normal."
  exit 1
fi

PREVIOUS_UPSTREAM="$(cat "$UPSTREAM_FILE" 2>/dev/null || echo '')"

echo "🎨 No ar agora:      ${ATUAL}"
echo "🎨 Voltará para:     ${DESTINO}  (imagem geosegbar-prod:previous)"
echo ""
read -r -p "Confirma o rollback? [s/N] " resposta
case "$resposta" in
  s|S|sim|SIM) ;;
  *) echo "Rollback cancelado."; exit 0 ;;
esac

# ------------------------------------------------------- sobe a versão anterior
echo ""
echo "🚀 Subindo a versão anterior em ${DESTINO}..."
docker rm -f "$DESTINO" >/dev/null 2>&1 || true

docker run -d \
  --name "$DESTINO" \
  --restart unless-stopped \
  --network geosegbar-network \
  --network-alias geosegbar-api-prod \
  --memory=2g \
  --cpus="1.5" \
  --expose 9090 \
  --env-file "$SCRIPT_DIR/.env.prod" \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}" \
  -e JAVA_OPTS="${JAVA_OPTS}" \
  -v "$SCRIPT_DIR/logs:/app/logs" \
  geosegbar-prod:previous

# --------------------------------------------------------------- espera saúde
TIMEOUT="${ROLLBACK_HEALTH_TIMEOUT_SECONDS:-300}"
ELAPSED=0
SAUDAVEL=false

echo "⏳ Aguardando ${DESTINO} ficar saudável (timeout ${TIMEOUT}s)..."
while [ "$ELAPSED" -lt "$TIMEOUT" ]; do
  if ! container_running "$DESTINO"; then
    echo "❌ O container da versão anterior parou sozinho durante o boot."
    break
  fi
  if docker exec "$DESTINO" \
       wget -q --spider -T 5 http://localhost:9090/actuator/health/readiness 2>/dev/null; then
    SAUDAVEL=true
    break
  fi
  echo "   ... ainda inicializando (${ELAPSED}s/${TIMEOUT}s)"
  sleep 5
  ELAPSED=$((ELAPSED + 5))
done

if [ "$SAUDAVEL" != true ]; then
  echo ""
  echo "❌ A versão anterior NÃO subiu. Nada foi trocado."
  echo "🛡️  ${ATUAL} continua no ar, exatamente como estava."
  echo ""
  echo "📋 Log da tentativa:"
  docker logs --tail 60 "$DESTINO" 2>&1 || true
  docker rm -f "$DESTINO" >/dev/null 2>&1 || true
  exit 1
fi

# ------------------------------------------------------------ troca o tráfego
echo "✅ ${DESTINO} saudável. Migrando tráfego..."
echo "set \$upstream_server ${DESTINO}:9090;" > "$UPSTREAM_FILE"

if ! docker exec nginx-prod nginx -t >/dev/null 2>&1; then
  echo "❌ Configuração do nginx inválida. Revertendo."
  [ -n "$PREVIOUS_UPSTREAM" ] && echo "$PREVIOUS_UPSTREAM" > "$UPSTREAM_FILE"
  docker rm -f "$DESTINO" >/dev/null 2>&1 || true
  exit 1
fi

docker exec nginx-prod nginx -s reload
echo "✅ Tráfego migrado para ${DESTINO} (reload gracioso)"

# ------------------------------------------------------------- encerra a atual
DRAIN="${ROLLBACK_DRAIN_SECONDS:-15}"
echo "🚪 Aguardando ${DRAIN}s para as requisições em andamento terminarem..."
sleep "$DRAIN"

docker stop "$ATUAL" >/dev/null 2>&1 || true
docker rm "$ATUAL" >/dev/null 2>&1 || true

echo ""
echo "✅ Rollback concluído. Versão anterior no ar em ${DESTINO}."
echo ""
echo "⚠️  ATENÇÃO — o banco NÃO foi revertido."
echo "   Migrações aplicadas pela versão com problema continuam valendo. Se o"
echo "   problema for de schema, avalie o backup pré-deploy em backups/."
echo ""
docker ps --filter "name=geosegbar" --filter "name=nginx-prod"
