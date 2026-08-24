package com.geosegbar.unit.configs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.geosegbar.common.enums.AuthErrorCodeEnum;
import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.exceptions.InvalidTokenException;
import com.geosegbar.exceptions.TokenExpiredException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.exceptions.exception_handler.RestExceptionHandler;

/**
 * Trava o contrato que o front depende:
 *
 *   401 -> não autenticado / sessão expirada -> deslogar
 *   403 -> autenticado, sem permissão        -> só avisar
 *
 * Antes desta padronização os dois estavam invertidos: falta de permissão subia
 * como 401 e requisição sem token era respondida com 403 pelo Spring Security.
 */
@Tag("unit")
@DisplayName("Unit Tests - Contrato de status 401 vs 403")
class AuthStatusContractTest extends BaseUnitTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    @DisplayName("Falta de autenticação responde 401 com NOT_AUTHENTICATED")
    void unauthorizedShouldBe401() {
        ResponseEntity<WebResponseEntity<String>> response
                = handler.handleUnauthorizedException(new UnauthorizedException("Usuário não autenticado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo(AuthErrorCodeEnum.NOT_AUTHENTICATED.name());
    }

    @Test
    @DisplayName("Falta de permissão responde 403 com FORBIDDEN — nunca 401")
    void forbiddenShouldBe403() {
        ResponseEntity<WebResponseEntity<String>> response
                = handler.handleForbiddenException(
                        new ForbiddenException("Usuário não tem permissão para editar instrumentos!"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(AuthErrorCodeEnum.FORBIDDEN.name());
    }

    @Test
    @DisplayName("Token expirado responde 401 com SESSION_EXPIRED")
    void expiredTokenShouldBe401SessionExpired() {
        ResponseEntity<WebResponseEntity<String>> response
                = handler.handleTokenException(new TokenExpiredException("Token expirado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo(AuthErrorCodeEnum.SESSION_EXPIRED.name());
    }

    @Test
    @DisplayName("Token inválido responde 401 com INVALID_TOKEN")
    void invalidTokenShouldBe401InvalidToken() {
        ResponseEntity<WebResponseEntity<String>> response
                = handler.handleInvalidTokenException(new InvalidTokenException("Token inválido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo(AuthErrorCodeEnum.INVALID_TOKEN.name());
    }

    @Test
    @DisplayName("Todo código de erro de sessão pertence ao grupo que desloga; FORBIDDEN não")
    void errorCodeGrouping() {
        List<AuthErrorCodeEnum> deslogam = List.of(
                AuthErrorCodeEnum.NOT_AUTHENTICATED,
                AuthErrorCodeEnum.SESSION_EXPIRED,
                AuthErrorCodeEnum.INVALID_TOKEN,
                AuthErrorCodeEnum.ACCOUNT_UNAVAILABLE);

        assertThat(deslogam).doesNotContain(AuthErrorCodeEnum.FORBIDDEN);
        // Se um código novo entrar no enum, este teste falha e obriga a decidir
        // conscientemente de que lado ele fica.
        assertThat(AuthErrorCodeEnum.values()).hasSize(deslogam.size() + 1);
    }

    @Test
    @DisplayName("Desconexão do cliente não vira alerta de erro inesperado")
    void clientDisconnectIsNotAnUnexpectedError() {
        // Broken pipe em download é o cliente indo embora, não falha do servidor.
        // Antes caía no handler genérico e disparava e-mail de "Erro Inesperado
        // Detectado" — alerta que dispara para coisa normal treina a equipe a
        // ignorar alerta.
        var request = new org.springframework.mock.web.MockHttpServletRequest(
                "GET", "/share/token-qualquer/files/30");

        assertThatCode(() -> handler.handleClientDisconnect(
                new org.apache.catalina.connector.ClientAbortException("Broken pipe"), request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Resposta de sucesso não carrega errorCode")
    void successHasNoErrorCode() {
        WebResponseEntity<String> ok = WebResponseEntity.success("dado", "Tudo certo!");

        assertThat(ok.isSuccess()).isTrue();
        assertThat(ok.getErrorCode()).isNull();
    }
}
