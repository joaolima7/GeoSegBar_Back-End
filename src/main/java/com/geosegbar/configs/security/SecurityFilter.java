package com.geosegbar.configs.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.geosegbar.common.enums.AuthErrorCodeEnum;
import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.configs.security.TokenService.TokenVerification;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.audit.filter.AuditLogFilter;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    /**
     * Motivo da falha de autenticação, repassado ao
     * {@link RestAuthenticationEntryPoint} pela request. Filtro não consegue
     * usar o @RestControllerAdvice — ele roda antes do DispatcherServlet — então
     * o caminho é registrar o motivo aqui e deixar o entry point responder.
     */
    public static final String AUTH_ERROR_CODE = "geosegbar.auth.errorCode";

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = recoverToken(request);
        TokenVerification verification = tokenService.verifyToken(token);

        if (!verification.isValid()) {
            // Não interrompe a cadeia: rotas públicas continuam funcionando sem token.
            // Se a rota exigir autenticação, o Spring Security aciona o entry point,
            // que lê este atributo para responder 401 com o motivo certo.
            request.setAttribute(AUTH_ERROR_CODE, verification.error());
            filterChain.doFilter(request, response);
            return;
        }

        Optional<UserEntity> found = userRepository.findByIdWithAllPermissions(verification.userId());

        if (found.isEmpty()) {
            // Token íntegro de uma conta que não existe mais. Antes isso subia uma
            // NotFoundException de dentro do filtro e virava 500 com página de erro
            // do container, porque o handler de exceções não alcança filtros.
            request.setAttribute(AUTH_ERROR_CODE, AuthErrorCodeEnum.ACCOUNT_UNAVAILABLE);
            filterChain.doFilter(request, response);
            return;
        }

        UserEntity user = found.get();

        if (user.getStatus() != null && user.getStatus().getStatus() == StatusEnum.DISABLED) {
            // Conta desativada depois do login não pode continuar navegando com o
            // token antigo.
            request.setAttribute(AUTH_ERROR_CODE, AuthErrorCodeEnum.ACCOUNT_UNAVAILABLE);
            filterChain.doFilter(request, response);
            return;
        }

        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Persiste o usuário autenticado em atributos da request para que a
        // auditoria (que monta o registro no finally, após o SecurityContext já
        // ter sido limpo) consiga vincular o ator ao ID correto.
        request.setAttribute(AuditLogFilter.AUDIT_ACTOR_USER_ID, user.getId());

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring("Bearer ".length()).trim();
    }
}
