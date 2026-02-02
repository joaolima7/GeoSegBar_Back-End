package com.geosegbar.configs.ratelimit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.RateLimitExceededException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interceptor que aplica rate limiting em todas as requisições HTTP.
 * <p>
 * Estratégia híbrida: - Requisições autenticadas: limita por User ID -
 * Requisições não autenticadas: limita por endereço IP
 * <p>
 * Adiciona headers informativos na resposta: - X-RateLimit-Limit: capacidade
 * total do bucket - X-RateLimit-Remaining: tokens restantes -
 * X-RateLimit-Reset: tempo em segundos até o próximo refill
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Ignora requisições OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Verifica se rate limiting está habilitado
        if (!rateLimitService.isEnabled()) {
            return true;
        }

        // Determina tipo de rate limit e identificador
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserEntity;

        String identifier;
        RateLimitType type;

        if (isAuthenticated) {
            // Usuário autenticado - limita por User ID
            UserEntity user = (UserEntity) authentication.getPrincipal();
            identifier = String.valueOf(user.getId());
            type = RateLimitType.AUTHENTICATED;

            log.debug("Rate limit check - Authenticated user: {}, ID: {}", user.getEmail(), identifier);
        } else {
            // Usuário não autenticado - limita por IP
            identifier = extractClientIp(request);
            type = RateLimitType.PUBLIC;

            log.debug("Rate limit check - Public request from IP: {}", identifier);
        }

        // Tenta consumir token
        RateLimitInfo rateLimitInfo = rateLimitService.tryConsume(identifier, type);

        // Adiciona headers informativos na resposta
        addRateLimitHeaders(response, rateLimitInfo);

        if (!rateLimitInfo.isAllowed()) {
            // Rate limit excedido - lança exceção que será tratada pelo RestExceptionHandler
            log.warn("Rate limit exceeded - Type: {}, Identifier: {}, Retry in: {}s",
                    type, identifier, rateLimitInfo.getSecondsUntilRefill());

            throw new RateLimitExceededException(
                    String.format("Taxa de requisições excedida. Tente novamente em %d segundos.",
                            rateLimitInfo.getSecondsUntilRefill()),
                    rateLimitInfo.getRemainingTokens(),
                    rateLimitInfo.getSecondsUntilRefill(),
                    rateLimitInfo.getCapacity()
            );
        }

        return true;
    }

    /**
     * Adiciona headers de rate limit na resposta HTTP.
     *
     * @param response Resposta HTTP
     * @param info Informações sobre o rate limit
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitInfo info) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(info.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.getSecondsUntilRefill()));
    }

    /**
     * Extrai o endereço IP real do cliente, considerando proxies e load
     * balancers.
     * <p>
     * Verifica os seguintes headers em ordem: 1. X-Forwarded-For (padrão de
     * proxies) 2. X-Real-IP (Nginx) 3. RemoteAddr (direto)
     *
     * @param request Requisição HTTP
     * @return Endereço IP do cliente
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For pode conter múltiplos IPs separados por vírgula
            // O primeiro é o IP real do cliente
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // Fallback para RemoteAddr
        ip = request.getRemoteAddr();

        // Normaliza localhost IPv6 para IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
