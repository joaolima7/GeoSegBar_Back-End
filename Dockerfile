
FROM maven:3.9.6-eclipse-temurin-21 AS build


WORKDIR /app


COPY pom.xml .
COPY src/main/resources/application.properties src/main/resources/


RUN mvn dependency:go-offline -B


COPY src ./src


RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine


RUN apk update && apk upgrade && \
    apk add --no-cache bash curl wget && \
    rm -rf /var/cache/apk/*


ENV TZ=America/Sao_Paulo


RUN apk update && apk upgrade && \
    apk add --no-cache bash curl wget tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    rm -rf /var/cache/apk/*


RUN addgroup -g 1001 -S springboot && \
    adduser -u 1001 -S springboot -G springboot


WORKDIR /app


RUN mkdir -p /home/wwgeomprod/public_html/storage/app/public/psb && \
    chown -R springboot:springboot /home/wwgeomprod


COPY --from=build /app/target/geosegbar-*.jar app.jar


# Padrão usado quando JAVA_OPTS não vem do ambiente. MaxRAMPercentage em vez de
# -Xmx fixo: a JVM se dimensiona pelo limite do cgroup, então a mesma imagem se
# comporta bem num container de 2 GB e num de 8 GB. Xmx fixo igual ao limite do
# container é armadilha — metaspace, pilhas de thread e buffers diretos vivem
# FORA do heap, e o kernel mata o container antes de a JVM atingir o próprio teto.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

# Flags de segurança aplicadas SEMPRE, depois de JAVA_OPTS, para valerem mesmo
# quando o ambiente sobrescreve JAVA_OPTS com uma configuração incompleta.
#
# ExitOnOutOfMemoryError é a mais importante. Uma JVM que sofre OutOfMemoryError
# fica em estado indefinido: em 24/08/2026, em homologação, o processo continuou
# vivo mas sem socket em escuta e sem consumir CPU — 18 horas fora do ar sem
# reiniciar. Morrer na hora deixa o `--restart unless-stopped` do Docker fazer o
# trabalho, e o prejuízo vira um restart de ~40 segundos.
ENV JAVA_SAFETY_OPTS="-XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"


EXPOSE 9090


USER springboot


ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS $JAVA_SAFETY_OPTS -jar app.jar --server.port=9090"]


# Sonda de liveness, não o /actuator/health composto: o health agregado fica DOWN
# quando qualquer dependência oscila, o que reiniciaria o container por um soluço
# do banco. liveness responde "o processo está vivo", que é o que o Docker precisa
# saber. start-period generoso porque o boot roda migração de banco.
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD wget --no-verbose --tries=1 -O /dev/null http://localhost:9090/actuator/health/liveness || exit 1