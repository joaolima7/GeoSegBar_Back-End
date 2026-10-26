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
import com.geosegbar.configs.filters.RequestCorrelationFilter;
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
                // Renovação de sessão (V4-47). Pública **de propósito**: o
                // token apresentado aqui pode estar vencido, e o SecurityFilter
                // o recusaria antes do controller — o que deixaria o endpoint
                // inútil justamente para quem precisa dele. Quem autentica é
                // UserService.refreshSession, que exige assinatura válida,
                // token igual ao lastToken (rotação), autenticação original
                // dentro de 30 dias e conta ativa.
                .requestMatchers(HttpMethod.POST, "/user/refresh-token").permitAll()
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
                // O actuator mora na porta de management (8081), que o nginx
                // nunca encaminha, e publica só health, prometheus e info
                // (management.endpoints.web.exposure.include). Esta cadeia de
                // segurança também vale para a porta de management, então a
                // liberação continua explícita e restrita a esses três: um
                // curinga sobre /actuator/** liberaria o que um dia fosse
                // exposto por engano, como /actuator/env e /actuator/configprops,
                // que carregam segredos. O nginx ainda nega /actuator/ na 9090.
                //
                // Sem restrição de método, de propósito: `wget --spider` — usado
                // tanto pelo HEALTHCHECK do Docker quanto pelo gate do deploy —
                // envia HEAD, não GET. Com o matcher preso a GET, o HEAD caía em
                // anyRequest().authenticated() e voltava 401: a aplicação subia
                // saudável e o deploy a rejeitava assim mesmo.
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/info").permitAll()
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

        // Códigos de correlação (RequestCorrelationFilter). O front pode mostrá-los
        // numa tela de erro para o suporte achar a requisição exata no Grafana.
        configuration.setExposedHeaders(List.of(
                RequestCorrelationFilter.REQUEST_ID_HEADER,
                RequestCorrelationFilter.TRACE_ID_HEADER));

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Ordem: ÚLTIMO filtro, de propósito explícito.
     *
     * O valor antigo era HIGHEST_PRECEDENCE - 1, que estoura (Integer.MIN_VALUE - 1
     * vira Integer.MAX_VALUE): o filtro sempre rodou por último, não primeiro. É
     * esse o comportamento em produção, e ele fica, porque protege: a
     * descompressão lê o corpo inteiro para a memória, sem teto de tamanho, e
     * rodando por último ela só acontece depois que a cadeia de segurança
     * deixou a requisição passar.
     *
     * Efeito colateral conhecido: o RequestBodyCachingFilter e a auditoria, que
     * rodam antes, guardam o corpo ainda compactado. Para descompactar primeiro,
     * é preciso antes pôr um teto de tamanho no GzipRequestWrapper — senão um
     * corpo pequeno que infla para GBs derruba a JVM sem autenticação.
     */
    @Bean
    public FilterRegistrationBean<GzipRequestDecompressingFilter> gzipRequestDecompressingFilter() {
        FilterRegistrationBean<GzipRequestDecompressingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipRequestDecompressingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
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
