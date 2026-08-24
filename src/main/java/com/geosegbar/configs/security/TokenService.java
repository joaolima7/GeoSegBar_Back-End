package com.geosegbar.configs.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.geosegbar.common.enums.AuthErrorCodeEnum;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.JWTException;

import jakarta.annotation.PostConstruct;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    private Algorithm algorithm;

    private static final String ISSUER = "GeoSegBar";
    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.of("-3");

    @PostConstruct
    public void init() {

        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(UserEntity user) {
        try {

            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getEmail())
                    .withClaim("id", user.getId())
                    .withClaim("role", user.getRole().getName().toString())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new JWTException("Erro ao gerar token!");
        }
    }

    /**
     * Resultado da verificação do token. Existe para separar "expirou" de
     * "inválido": os dois devolvem 401, mas só o primeiro justifica dizer ao
     * usuário que a sessão acabou — o segundo costuma ser token corrompido no
     * storage do navegador.
     */
    public record TokenVerification(Long userId, AuthErrorCodeEnum error) {

        public boolean isValid() {
            return userId != null;
        }

        static TokenVerification valid(Long userId) {
            return new TokenVerification(userId, null);
        }

        static TokenVerification failed(AuthErrorCodeEnum error) {
            return new TokenVerification(null, error);
        }
    }

    /**
     * Verifica o token e diz quem é o usuário ou por que falhou.
     *
     * O ID vem do claim "id", nunca do e-mail (subject), que é mutável:
     * resolver por ID garante que uma troca de e-mail não invalide um token já
     * emitido (a busca do usuário autenticado é sempre por ID, ver
     * {@code SecurityFilter}).
     */
    public TokenVerification verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return TokenVerification.failed(AuthErrorCodeEnum.NOT_AUTHENTICATED);
        }
        try {
            Long userId = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token)
                    .getClaim("id")
                    .asLong();

            return userId != null
                    ? TokenVerification.valid(userId)
                    : TokenVerification.failed(AuthErrorCodeEnum.INVALID_TOKEN);
        } catch (TokenExpiredException exception) {
            return TokenVerification.failed(AuthErrorCodeEnum.SESSION_EXPIRED);
        } catch (JWTVerificationException exception) {
            return TokenVerification.failed(AuthErrorCodeEnum.INVALID_TOKEN);
        }
    }

    /**
     * Atalho para quem só precisa do ID e não se importa com o motivo da falha.
     */
    public Long getUserIdFromToken(String token) {
        return verifyToken(token).userId();
    }

    public boolean isTokenValid(String token) {
        if (token == null) {
            return false;
        }
        try {
            JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
            return true;
        } catch (JWTVerificationException exception) {
            return false;
        }
    }

    private Instant generateExpirationDate() {

        return LocalDateTime.now().plusHours(12).toInstant(ZONE_OFFSET);
    }
}
