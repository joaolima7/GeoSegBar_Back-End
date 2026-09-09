package com.geosegbar.configs.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
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

    /**
     * Quanto vale um token de acesso.
     *
     * Doze horas é o que sempre valeu; o que mudou no V4-47 é existir
     * {@link #generateRefreshedToken} para renová-lo antes do fim, em vez de a
     * sessão simplesmente morrer.
     */
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(12);

    /**
     * Por quanto tempo, contado da autenticação ORIGINAL, uma sessão pode ser
     * renovada sem o usuário digitar a senha de novo.
     *
     * Trinta dias, e o número não é arbitrário: é a mesma janela que o MFA já
     * usa na web ({@code UserService.initiateLogin}, "pula MFA se já verificou
     * nos últimos 30 dias"). Ter as duas janelas iguais evita o absurdo de uma
     * sessão sobreviver mais tempo do que a verificação que a autorizou.
     *
     * É o que impede a renovação de virar sessão eterna: um token roubado vale,
     * no pior caso, o que resta destes trinta dias — não para sempre.
     */
    public static final Duration MAX_SESSION_AGE = Duration.ofDays(30);

    /**
     * Instante da autenticação original, em epoch segundos. Renovar
     * **preserva** este claim; só um login novo o reinicia.
     */
    private static final String CLAIM_AUTH_TIME = "auth_time";

    @PostConstruct
    public void init() {

        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(UserEntity user) {
        return buildToken(user, Instant.now());
    }

    /**
     * Emite um token novo **preservando o instante da autenticação original**.
     *
     * É o que separa "renovar" de "logar de novo": o relógio dos trinta dias
     * de {@link #MAX_SESSION_AGE} continua correndo do login que realmente
     * aconteceu, e não de cada renovação — senão a janela nunca fecharia e a
     * sessão seria eterna, que é exatamente o que ela existe para impedir.
     */
    public String generateRefreshedToken(UserEntity user, Instant authTime) {
        return buildToken(user, authTime);
    }

    private String buildToken(UserEntity user, Instant authTime) {
        try {
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getEmail())
                    .withClaim("id", user.getId())
                    .withClaim("role", user.getRole().getName().toString())
                    .withClaim(CLAIM_AUTH_TIME, authTime.getEpochSecond())
                    .withExpiresAt(Instant.now().plus(ACCESS_TOKEN_TTL))
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

    /**
     * O que um token vencido ainda pode provar.
     *
     * @param userId quem ele identifica
     * @param authTime quando aquela sessão começou de verdade; {@code null}
     * para tokens emitidos antes de o claim existir
     */
    public record ExpiredTokenClaims(Long userId, Instant authTime) {

    }

    /**
     * Verifica um token **ignorando a expiração**, para a renovação.
     *
     * ⚠️ Parece perigoso e não é — desde que quem chama cobre o resto, e
     * {@code UserService.refreshSession} cobra:
     *
     * <ol>
     * <li>a <b>assinatura</b> e o emissor continuam sendo verificados aqui: um
     * token forjado não passa;</li>
     * <li>o token apresentado tem de ser <b>o último emitido</b> para aquele
     * usuário ({@code lastToken}), o que dá rotação e revogação — trocar de
     * aparelho ou logar de novo invalida o anterior;</li>
     * <li>a autenticação original tem de estar dentro de
     * {@link #MAX_SESSION_AGE}.</li>
     * </ol>
     *
     * Sem ignorar o {@code exp} não haveria renovação possível para o caso que
     * mais importa: o inspetor que passou a noite fora do app e volta com o
     * token vencido por poucas horas. Exigir um token válido para pedir um
     * token novo só serviria a quem já não precisava.
     */
    public ExpiredTokenClaims verifyIgnoringExpiration(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            DecodedJWT decoded = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .acceptExpiresAt(Long.MAX_VALUE / 1000)
                    .build()
                    .verify(token);

            Long userId = decoded.getClaim("id").asLong();
            if (userId == null) {
                return null;
            }

            Claim authTime = decoded.getClaim(CLAIM_AUTH_TIME);
            return new ExpiredTokenClaims(
                    userId,
                    authTime.isMissing() || authTime.isNull()
                            ? null
                            : Instant.ofEpochSecond(authTime.asLong())
            );
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}
