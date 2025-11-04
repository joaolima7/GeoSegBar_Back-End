package com.geosegbar.configs.redis;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    // ⭐ CORREÇÃO: Usar Duration ao invés de long
    @Value("${spring.data.redis.timeout:2000ms}")
    @DurationUnit(ChronoUnit.MILLIS)
    private Duration redisTimeout;

    /**
     * ClientResources com configuração otimizada
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(4)
                .computationThreadPoolSize(4)
                .build();
    }

    /**
     * LettuceClientConfiguration com proteção contra read-only replica
     */
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration(ClientResources clientResources) {

        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(3)))
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .keepAlive(true)
                        .build())
                .build();

        return LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .clientResources(clientResources)
                .commandTimeout(redisTimeout) // ✅ Agora funciona com Duration
                .readFrom(ReadFrom.UPSTREAM)
                .build();
    }

    /**
     * RedisConnectionFactory com configuração standalone segura
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            LettuceClientConfiguration lettuceClientConfiguration) {

        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        standaloneConfig.setHostName(redisHost);
        standaloneConfig.setPort(redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            standaloneConfig.setPassword(redisPassword);
        }

        standaloneConfig.setDatabase(0);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                standaloneConfig,
                lettuceClientConfiguration
        );

        factory.setValidateConnection(true);
        factory.setShareNativeConnection(false);

        return factory;
    }

    /**
     * RedisTemplate com serialização JSON otimizada
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.setEnableTransactionSupport(false);
        template.afterPropertiesSet();

        return template;
    }
}
