
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


ENV JAVA_OPTS="-Xms512m -Xmx1280m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"


EXPOSE 9090


USER springboot


ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=9090"]


HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || exit 1