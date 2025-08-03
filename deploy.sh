PROJECT_DIR="$HOME/backend.geometrisa-prod.com.br"
TARGET_DIR="$PROJECT_DIR/target"
JAR_NAME="geosegbar-0.0.1-SNAPSHOT.jar"
PORT=9090
LOG_FILE="$TARGET_DIR/log.out"
WAIT_TIME=20  

set -euo pipefail

echo "🚀 Iniciando deploy em PRODUÇÃO com OpenTelemetry em $(date '+%Y-%m-%d %H:%M:%S')"

echo "→ git pull"
cd "$PROJECT_DIR"
git pull origin main

echo "→ mvn clean package"
mvn clean package -q

echo "→ procurando processo antigo na porta $PORT"
PID=$(lsof -t -i :"$PORT" || true)
if [ -n "$PID" ]; then
  echo "  Encontrado PID $PID. Matando..."
  kill "$PID"
  sleep 2
else
  echo "  Nenhum processo rodando na porta $PORT."
fi

echo "→ iniciando jar em PRODUÇÃO com observabilidade (nohup)"
cd "$TARGET_DIR"

nohup java -jar \
  -Dspring.profiles.active=prod \
  -Xms512m \
  -Xmx2g \
  "$JAR_NAME" \
  --server.port="$PORT" > "$LOG_FILE" 2>&1 &

echo "→ aguardando $WAIT_TIME segundos para boot"
sleep "$WAIT_TIME"

echo -n "→ checando se aplicação subiu corretamente... "
if lsof -t -i :"$PORT" >/dev/null; then
  echo "✅ OK! Processo rodando na porta $PORT com OpenTelemetry ativo."
  echo "📊 Dados sendo enviados para Axiom dataset: geosegbar"
  echo "📋 Logs em: $LOG_FILE"
  echo "🔍 Health check: http://backend.geometrisa-prod.com.br:$PORT/actuator/health"
  exit 0
else
  echo "❌ FALHA! Não há processo na porta $PORT."
  echo "Verifique o log: $LOG_FILE"
  exit 1
fi