#!/bin/bash
cd /home/wwgeomprod/backend.geometrisa-prod.com.br/target

# Configurações CONSERVADORAS - que funcionaram anteriormente
JAVA_OPTS="-XX:MetaspaceSize=128M -XX:MaxMetaspaceSize=256M -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xss256k"

nohup java $JAVA_OPTS -jar geosegbar-0.0.1-SNAPSHOT.jar --server.port=9090 > log.out 2>&1 &

echo "Aplicação iniciada com PID: $!"