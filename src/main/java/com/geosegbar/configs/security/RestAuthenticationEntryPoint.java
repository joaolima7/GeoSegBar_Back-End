package com.geosegbar.configs.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosegbar.common.enums.AuthErrorCodeEnum;
import com.geosegbar.common.response.WebResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Responde 401 quando a requisição chega sem autenticação válida a uma rota
 * protegida.
 *
 * Sem este bean o Spring Security usa o Http403ForbiddenEntryPoint e devolve
 * 403 para quem apenas não está autenticado — que era exatamente o que fazia o
 * front confundir sessão expirada com falta de permissão.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        AuthErrorCodeEnum errorCode = resolveErrorCode(request);

        log.debug("[AUTH] 401 em {} {} — motivo: {}", request.getMethod(), request.getRequestURI(), errorCode);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
                response.getOutputStream(),
                WebResponseEntity.error(messageFor(errorCode), errorCode));
    }

    private AuthErrorCodeEnum resolveErrorCode(HttpServletRequest request) {
        Object attribute = request.getAttribute(SecurityFilter.AUTH_ERROR_CODE);
        if (attribute instanceof AuthErrorCodeEnum code) {
            return code;
        }
        return AuthErrorCodeEnum.NOT_AUTHENTICATED;
    }

    private String messageFor(AuthErrorCodeEnum errorCode) {
        return switch (errorCode) {
            case SESSION_EXPIRED ->
                "Sua sessão expirou. Entre novamente para continuar.";
            case INVALID_TOKEN ->
                "Sessão inválida. Entre novamente para continuar.";
            case ACCOUNT_UNAVAILABLE ->
                "Esta conta não está mais disponível. Contate o administrador do sistema.";
            default ->
                "É necessário estar autenticado para acessar este recurso.";
        };
    }
}
