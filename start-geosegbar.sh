#!/bin/bash
cd /home/wwgeomprod/backend.geometrisa-prod.com.br/target

# Parar processo anterior se existir
pkill -f geosegbar || true
sleep 3

# Limpar cache do sistema
sync
echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true

# Configurações CONSERVADORAS - que funcionaram anteriormente
JAVA_OPTS="-XX:MetaspaceSize=128M -XX:MaxMetaspaceSize=256M -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss256k"

nohup java $JAVA_OPTS -jar geosegbar-0.0.1-SNAPSHOT.jar --server.port=9090 > log.out 2>&1 &

echo "Aplicação iniciada com PID: $!"