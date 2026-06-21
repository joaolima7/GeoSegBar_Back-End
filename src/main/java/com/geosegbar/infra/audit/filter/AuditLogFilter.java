package com.geosegbar.infra.audit.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.common.utils.ClientIpUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.audit.config.AuditProperties;
import com.geosegbar.infra.audit.services.AuditContext;
import com.geosegbar.infra.audit.services.AuditService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Captura automaticamente uma auditoria por requisição HTTP, sem depender de
 * nada vindo do front. Envolve a resposta em {@link ContentCachingResponseWrapper}
 * para ler status e body (incl. a {@code message} do {@code WebResponseEntity}).
 * <p>
 * Regras (configuráveis via {@link AuditProperties}):
 * <ul>
 *   <li>Audita POST/PUT/PATCH/DELETE e rotas de login; GET apenas se habilitado.</li>
 *   <li>Ignora OPTIONS, estáticos e rotas excluídas.</li>
 *   <li>Request/response body são gravados somente quando o resultado é ERRO.</li>
 * </ul>
 * É best-effort: qualquer exceção na auditoria é engolida para não afetar a resposta.
 */
@RequiredArgsConstructor
@Slf4j
public class AuditLogFilter extends OncePerRequestFilter {

    private static final String LOGIN_INITIATE_PATH = "/user/login/initiate";
    private static final String LOGIN_VERIFY_PATH = "/user/login/verify";

    private final AuditService auditService;
    private final AuditProperties auditProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!auditProperties.isEnabled() || !shouldAudit(request)) {
            log.info("[AUDIT] filtro PULOU {} {} (enabled={})",
                    request.getMethod(), request.getRequestURI(), auditProperties.isEnabled());
            filterChain.doFilter(request, response);
            return;
        }

        log.info("[AUDIT] filtro VAI AUDITAR {} {}", request.getMethod(), request.getRequestURI());

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        long startNanos = System.nanoTime();

        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            try {
                long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
                buildAndRecord(request, responseWrapper, durationMs);
            } catch (Exception e) {
                log.error("[AUDIT] Falha ao auditar requisição {} {}: {}",
                        request.getMethod(), request.getRequestURI(), e.getMessage(), e);
            } finally {
                // Sempre devolve o corpo bufferizado ao cliente.
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }

        String path = request.getRequestURI();
        if (path != null) {
            for (String excluded : auditProperties.getExcludedPaths()) {
                if (path.startsWith(excluded)) {
                    return false;
                }
            }
        }

        boolean isMutation = "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);

        if (isMutation) {
            return true;
        }

        // GET (e demais) só se explicitamente habilitado.
        return auditProperties.isIncludeGet();
    }

    private void buildAndRecord(HttpServletRequest request,
            ContentCachingResponseWrapper responseWrapper, long durationMs) {

        int httpStatus = responseWrapper.getStatus();
        String responseBody = readResponseBody(responseWrapper);

        // Mensagem e sucesso vêm do WebResponseEntity (campos message/success).
        String message = null;
        Boolean successFlag = null;
        JsonNode responseJson = parseJson(responseBody);
        if (responseJson != null) {
            if (responseJson.hasNonNull("message")) {
                message = responseJson.get("message").asText();
            }
            if (responseJson.has("success") && responseJson.get("success").isBoolean()) {
                successFlag = responseJson.get("success").asBoolean();
            }
        }

        AuditStatus status = resolveStatus(httpStatus, successFlag);
        boolean isError = status == AuditStatus.ERROR;

        String path = request.getRequestURI();
        String queryString = request.getQueryString();

        AuditContext.AuditContextBuilder ctx = AuditContext.builder()
                .action(buildAction(request.getMethod(), path))
                .actionLabel(buildActionLabel(request.getMethod(), path))
                .source(AuditSource.HTTP)
                .status(status)
                .message(message)
                .durationMs(durationMs)
                .httpMethod(request.getMethod())
                .endpoint(path)
                .queryString(queryString)
                .httpStatus(httpStatus)
                .clientIp(ClientIpUtil.extractClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .origin(resolveOrigin(request));

        applyActor(ctx, request, path);

        // Bodies apenas em erro (para depuração), com mascaramento.
        if (isError) {
            ctx.requestBody(maskJson(readRequestBody(request)));
            ctx.responseBody(maskJson(responseBody));
            ctx.requestHeaders(buildMaskedHeaders(request));
        }

        auditService.record(ctx.build());
    }

    private AuditStatus resolveStatus(int httpStatus, Boolean successFlag) {
        if (httpStatus >= 400) {
            return AuditStatus.ERROR;
        }
        if (Boolean.FALSE.equals(successFlag)) {
            return AuditStatus.ERROR;
        }
        return AuditStatus.SUCCESS;
    }

    /**
     * Resolve o ator gravando APENAS a FK do usuário (nome/e-mail/role vêm de
     * JOIN na leitura). Para a maioria das rotas usa o {@code SecurityContext}.
     * Nas rotas de login o usuário ainda não está autenticado (e em login que
     * falha nunca estará): extrai o e-mail tentado do corpo da requisição e o
     * grava como rótulo de fallback ({@code actorLabel}).
     */
    private void applyActor(AuditContext.AuditContextBuilder ctx, HttpServletRequest request, String path) {
        UserEntity user = resolveCurrentUser();
        if (user != null) {
            ctx.actorUserId(user.getId());
            return;
        }

        if (isLoginPath(path)) {
            String email = extractLoginEmail(request);
            if (email != null) {
                ctx.actorLabel(email);
            }
        }
    }

    private UserEntity resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserEntity user) {
                return user;
            }
        } catch (Exception ignored) {
            // sem contexto
        }
        return null;
    }

    private boolean isLoginPath(String path) {
        return LOGIN_INITIATE_PATH.equals(path) || LOGIN_VERIFY_PATH.equals(path);
    }

    /**
     * Extrai o campo "email" do corpo JSON da requisição de login a partir do
     * cache do {@code RequestBodyCachingFilter}, sem mascaramento (precisamos
     * registrar QUEM tentou logar).
     */
    private String extractLoginEmail(HttpServletRequest request) {
        try {
            String body = readRequestBody(request);
            JsonNode node = parseJson(body);
            if (node != null && node.hasNonNull("email")) {
                return node.get("email").asText();
            }
        } catch (Exception ignored) {
            // ignora
        }
        return null;
    }

    private String resolveOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin;
        }
        return request.getHeader("Referer");
    }

    // ---- Leitura de bodies ----

    private String readRequestBody(HttpServletRequest request) {
        Object cached = request.getAttribute("cachedRequestBody");
        if (cached instanceof byte[] bytes && bytes.length > 0) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        // Fallback: ContentCachingRequestWrapper guarda o corpo já consumido.
        if (request instanceof org.springframework.web.util.ContentCachingRequestWrapper wrapper) {
            byte[] bytes = wrapper.getContentAsByteArray();
            if (bytes != null && bytes.length > 0) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String readResponseBody(ContentCachingResponseWrapper responseWrapper) {
        byte[] bytes = responseWrapper.getContentAsByteArray();
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Mascaramento ----

    /**
     * Mascara campos sensíveis em um JSON. Se não for JSON válido, retorna o
     * texto original (já limitado a tamanho na camada de serviço).
     */
    private String maskJson(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            maskNode(root);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return body;
        }
    }

    private void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode obj =
                    (com.fasterxml.jackson.databind.node.ObjectNode) node;
            obj.fieldNames().forEachRemaining(field -> {
                if (isMasked(field)) {
                    obj.put(field, "***");
                } else {
                    maskNode(obj.get(field));
                }
            });
        } else if (node.isArray()) {
            node.forEach(this::maskNode);
        }
    }

    private boolean isMasked(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        for (String masked : auditProperties.getMaskedFields()) {
            if (lower.equals(masked.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String buildMaskedHeaders(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames != null && headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String value = request.getHeader(name);
                if (isMasked(name)) {
                    value = "***";
                }
                sb.append(name).append(": ").append(value != null ? value : "").append("\n");
            }
        } catch (Exception e) {
            return "(erro ao ler headers)";
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ---- Ação / rótulo ----

    /**
     * Código de ação derivado de método + primeiro segmento do path, ex.:
     * POST /checklists/complete -> "POST_CHECKLISTS".
     */
    private String buildAction(String method, String path) {
        String resource = firstSegment(path);
        return (method + "_" + resource).toUpperCase();
    }

    /**
     * Rótulo amigável (PT-BR) para o usuário comum.
     */
    private String buildActionLabel(String method, String path) {
        String resource = firstSegment(path);
        String verb = switch (method.toUpperCase()) {
            case "POST" ->
                "Criar";
            case "PUT", "PATCH" ->
                "Atualizar";
            case "DELETE" ->
                "Excluir";
            case "GET" ->
                "Consultar";
            default ->
                method;
        };
        if (LOGIN_INITIATE_PATH.equals(path) || LOGIN_VERIFY_PATH.equals(path)) {
            return "Login";
        }
        return verb + " " + resource;
    }

    private String firstSegment(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        return slash > 0 ? trimmed.substring(0, slash) : trimmed;
    }
}
