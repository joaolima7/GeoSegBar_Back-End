# Estágio de construção
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Definir diretório de trabalho
WORKDIR /app

# Copiar arquivos de configuração do Maven primeiro (para cache de layers)
COPY pom.xml .
COPY src/main/resources/application.properties src/main/resources/

# Baixar dependências (será cacheado se o pom.xml não mudar)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Construir a aplicação
RUN mvn clean package -DskipTests

# Estágio de produção
FROM eclipse-temurin:21-jre-alpine

# Atualizar pacotes para corrigir vulnerabilidades
RUN apk update && apk upgrade && \
    apk add --no-cache bash curl wget && \
    rm -rf /var/cache/apk/*

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S springboot && \
    adduser -u 1001 -S springboot -G springboot

# Definir diretório de trabalho
WORKDIR /app

# Criar diretórios necessários (mantendo a estrutura original)
RUN mkdir -p /home/wwgeomprod/public_html/storage/app/public/psb && \
    chown -R springboot:springboot /home/wwgeomprod

# Copiar arquivo JAR do estágio de build
COPY --from=build /app/target/geosegbar-*.jar app.jar

# Definir propriedades do Java para otimização
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

# Expor porta
EXPOSE 9090

# Mudar para usuário não-root
USER springboot

# Comando de inicialização
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=9090"]

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || exit 1