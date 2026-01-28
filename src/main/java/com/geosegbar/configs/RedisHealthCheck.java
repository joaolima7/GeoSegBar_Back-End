package com.geosegbar.configs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RedisHealthCheck {

    /**
     * Testa conexão Redis na inicialização (apenas em dev)
     */
    @Bean
    @Profile("dev")
    public CommandLineRunner testRedisConnection(RedisTemplate<String, Object> redisTemplate) {
        return args -> {
            try {
                // Testa operação básica
                redisTemplate.opsForValue().set("health:check", "OK");
                String value = (String) redisTemplate.opsForValue().get("health:check");
                redisTemplate.delete("health:check");

                if ("OK".equals(value)) {
                    log.info("✅ Redis conectado com sucesso!");
                } else {
                    log.warn("⚠️  Redis respondeu mas valor inesperado: {}", value);
                }
            } catch (Exception e) {
                log.error("❌ Falha ao conectar no Redis: {}", e.getMessage());
                log.warn("⚠️  Jobs históricos NÃO funcionarão sem Redis!");
            }
        };
    }
}
