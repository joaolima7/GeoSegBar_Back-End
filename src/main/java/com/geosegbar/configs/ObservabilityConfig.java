package com.geosegbar.configs;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.geosegbar.configs.filters.RequestCorrelationFilter;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.tracing.Tracer;

/**
 * Peças de observabilidade da aplicação que não são autoconfiguradas pelo
 * Spring Boot. O desenho completo está em docs/MONITORAMENTO.md.
 */
@Configuration
public class ObservabilityConfig {

    /**
     * Ordem: depois do ServerHttpObservationFilter do Spring
     * (HIGHEST_PRECEDENCE + 1), que abre o span da requisição — sem ele aberto
     * não há traceId para devolver — e bem antes da cadeia do Spring Security
     * (-100), onde o SecurityFilter põe o userId no MDC.
     */
    static final int CORRELATION_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter(
            ObjectProvider<Tracer> tracerProvider,
            @Value("${management.tracing.enabled:true}") boolean traceExportEnabled) {
        FilterRegistrationBean<RequestCorrelationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestCorrelationFilter(tracerProvider, traceExportEnabled));
        registration.addUrlPatterns("/*");
        registration.setOrder(CORRELATION_FILTER_ORDER);
        return registration;
    }

    /**
     * {@code geosegbar_build_info{version="<sha>"} 1}. O deploy passa o SHA do
     * commit em APP_VERSION; os painéis marcam a troca de versão e a visão geral
     * mostra o que está no ar em cada instância (blue e green).
     */
    @Bean
    public MeterBinder buildInfoMetrics(@Value("${application.version:dev}") String version) {
        return registry -> Gauge.builder("geosegbar.build.info", () -> 1)
                .description("Versão da API em execução (valor sempre 1)")
                .tag("version", version)
                .register(registry);
    }
}
