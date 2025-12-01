package com.geosegbar.configs.jpa;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosegbar.configs.observability.QueryPerformanceInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class HibernateConfig {

    private final QueryPerformanceInterceptor queryInterceptor;

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.session_factory.statement_inspector", queryInterceptor);
        };
    }
}
