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
     * Verifica o token e retorna o ID do usuário (claim "id") — nunca o e-mail
     * (subject), que é mutável. Resolver por ID garante que uma troca de e-mail
     * jamais invalide um token já emitido (a busca do usuário autenticado é
     * sempre por ID, ver {@code SecurityFilter}).
     */
    public Long getUserIdFromToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token)
                    .getClaim("id")
                    .asLong();
        } catch (JWTVerificationException exception) {
            return null;
        }
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
