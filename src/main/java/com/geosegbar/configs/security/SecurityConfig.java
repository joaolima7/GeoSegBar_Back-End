package com.geosegbar.configs.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosegbar.configs.filters.GzipRequestDecompressingFilter;
import com.geosegbar.configs.filters.RequestBodyCachingFilter;
import com.geosegbar.infra.audit.config.AuditProperties;
import com.geosegbar.infra.audit.filter.AuditLogFilter;
import com.geosegbar.infra.audit.services.AuditService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    SecurityFilter securityFilter;

    @Autowired
    RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/user/login/initiate").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/login/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/verify-reset-code").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/password-setup/validate").permitAll()
                .requestMatchers(HttpMethod.POST, "/password-setup/complete").permitAll()
                // Compartilhamento publico de PSB: quem autoriza e o token do link,
                // validado em ShareFolderService. /psb/files/download NAO entra aqui —
                // e a rota interna e exige sessao; o publico usa /share/{token}/files.
                .requestMatchers(HttpMethod.GET, "/share/access/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/share/download/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/share/*/files/**").permitAll()
                // Sondas de saúde e métricas.
                //
                // Estavam sob anyRequest().authenticated() e respondiam 403: o
                // HEALTHCHECK do Docker falhou 29.238 vezes seguidas (container
                // "unhealthy" desde que subiu) e o Prometheus nunca coletou uma
                // métrica sequer — os alertas e dashboards estavam cegos.
                //
                // Libera-se APENAS health e prometheus. O resto do actuator
                // continua exigindo autenticação: com
                // management.endpoints.web.exposure.include=*, expor /actuator/**
                // publicaria /actuator/env e /actuator/configprops, que carregam
                // segredos. O nginx ainda bloqueia /actuator/ vindo da internet —
                // Prometheus e healthcheck alcançam pela rede interna do Docker.
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                .anyRequest().authenticated()
                )
                // Sem isto o Spring usa o Http403ForbiddenEntryPoint e devolve 403
                // para quem apenas não está autenticado, tornando impossível para o
                // front distinguir sessão expirada de falta de permissão.
                //   401 -> não autenticado / sessão expirada  -> front desloga
                //   403 -> autenticado, sem permissão          -> front só avisa
                .exceptionHandling(handling -> handling
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("*"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public FilterRegistrationBean<GzipRequestDecompressingFilter> gzipRequestDecompressingFilter() {
        FilterRegistrationBean<GzipRequestDecompressingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipRequestDecompressingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE - 1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestBodyCachingFilter> requestBodyCachingFilter() {
        FilterRegistrationBean<RequestBodyCachingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestBodyCachingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Filtro de auditoria. Registrado logo após o cache do corpo da requisição,
     * de forma que ele já tenha o body disponível e envolva a resposta antes do
     * restante da cadeia (Security, controllers).
     */
    @Bean
    public FilterRegistrationBean<AuditLogFilter> auditLogFilter(
            AuditService auditService, AuditProperties auditProperties, ObjectMapper objectMapper) {
        FilterRegistrationBean<AuditLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuditLogFilter(auditService, auditProperties, objectMapper));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
