package com.geosegbar.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.geosegbar.configs.ratelimit.RateLimitInterceptor;

import lombok.RequiredArgsConstructor;

/**
 * Configuração do Spring MVC para registrar interceptors customizados.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * Registra o interceptor de rate limiting para todas as requisições.
     * <p>
     * Endpoints excluídos do rate limiting: - /actuator/** (métricas e health
     * checks) - /uploads/** (arquivos estáticos)
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/uploads/**"
                );
    }
}
