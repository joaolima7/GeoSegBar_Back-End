package com.geosegbar.configs.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // ✅ ADICIONADO: Conecta a configuração de CORS ao Spring Security
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                // --- Endpoints de Monitoramento ---
                // CUIDADO: Em produção, isso expõe dados do servidor. 
                // Se possível, restrinja ou remova se não estiver usando ativamente.
                // .requestMatchers("/actuator/health/**").permitAll()
                // .requestMatchers("/actuator/info").permitAll()
                // .requestMatchers("/actuator/prometheus").permitAll()
                // .requestMatchers("/actuator/metrics/**").permitAll()

                // --- Autenticação ---
                .requestMatchers(HttpMethod.POST, "/user/login/initiate").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/login/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/verify-reset-code").permitAll()
                .requestMatchers(HttpMethod.POST, "/user/reset-password").permitAll()
                // --- Arquivos Públicos ---
                .requestMatchers(HttpMethod.GET, "/psb/files/download/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/share/access/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/share/download/**").permitAll()
                // Todo o resto exige autenticação
                .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ✅ CONFIGURAÇÃO DE CORS CENTRALIZADA
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ⚠️ ATENÇÃO: "*" permite qualquer origem. 
        // Para maior segurança em produção, troque "*" pela URL do seu front (ex: "https://meusite.com")
        configuration.setAllowedOrigins(List.of("*"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Permite todos os headers (Authorization, Content-Type, etc)
        configuration.setAllowedHeaders(List.of("*"));

        // Se usar "*" no allowedOrigins, allowCredentials DEVE ser false.
        // Se colocar o domínio específico do front, pode mudar para true (se usar cookies/auth complexa).
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
