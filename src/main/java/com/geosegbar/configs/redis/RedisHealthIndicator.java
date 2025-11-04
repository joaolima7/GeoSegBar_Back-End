package com.geosegbar.configs.redis;

import java.util.Properties;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Health health() {
        try {
            RedisConnection connection = connectionFactory.getConnection();

            // ✅ Testar comando PING
            String pong = connection.ping();

            // ✅ Verificar role (DEVE SER master)
            Properties info = connection.serverCommands().info("replication");
            String role = info.getProperty("role");

            connection.close();

            if (!"master".equals(role)) {
                log.error("❌ Redis está em modo: {} (esperado: master)", role);
                return Health.down()
                        .withDetail("role", role)
                        .withDetail("error", "Redis NÃO está em modo master! Aplicação NÃO PODE escrever cache!")
                        .build();
            }

            return Health.up()
                    .withDetail("ping", pong)
                    .withDetail("role", role)
                    .withDetail("status", "Master OK - Pronto para leitura/escrita")
                    .build();

        } catch (Exception e) {
            log.error("❌ Erro ao verificar saúde do Redis", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
