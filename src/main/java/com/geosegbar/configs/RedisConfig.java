package com.geosegbar.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    /**
     * Configura RedisTemplate para operações com Redis - Key: String
     * (serialização simples) - Value: JSON (serialização Jackson para objetos
     * complexos)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Serializer para chaves (String simples)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Serializer para valores (JSON com suporte a LocalDate, LocalDateTime, etc)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Suporte a java.time.*
        GenericJackson2JsonRedisSerializer jsonSerializer
                = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Configurar serializadores
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
