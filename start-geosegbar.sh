#!/bin/bash
JAVA_OPTS="-XX:MetaspaceSize=384M -XX:MaxMetaspaceSize=768M -Xms768m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

nohup java $JAVA_OPTS -jar geosegbar-0.0.1-SNAPSHOT.jar --server.port=9090 > log.out 2>&1 &

echo "Aplicação iniciada com PID: $!"