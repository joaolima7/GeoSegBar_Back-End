package com.geosegbar.infra.password_setup.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.PasswordSetupTokenEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.InvalidTokenException;
import com.geosegbar.exceptions.TokenExpiredException;
import com.geosegbar.infra.password_setup.dtos.CompletePasswordSetupDTO;
import com.geosegbar.infra.password_setup.dtos.PasswordSetupInfoDTO;
import com.geosegbar.infra.password_setup.persistence.jpa.PasswordSetupTokenRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fluxo de primeiro acesso sem credencial no e-mail.
 *
 * Em vez de gerar uma senha temporária e mandá-la em texto plano — conteúdo que
 * filtros de anti-phishing tratam como comprometimento de credencial e colocam
 * em quarentena — o sistema emite um token opaco de uso único e envia apenas um
 * link. O usuário define a própria senha dentro do sistema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordSetupService {

    /**
     * Prazo de validade do link. Longo o bastante para o usuário ver o e-mail
     * num fim de semana e curto o bastante para o link não ficar circulando.
     */
    private static final int TOKEN_VALIDITY_HOURS = 48;

    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordSetupTokenRepository passwordSetupTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Emite um novo link de definição de senha e o envia por e-mail. Qualquer
     * link pendente do mesmo usuário é invalidado, de modo que só exista um
     * válido por vez.
     */
    @Transactional
    public void issueAndSendSetupLink(UserEntity user, boolean welcome) {
        passwordSetupTokenRepository.invalidatePendingTokens(user.getId(), LocalDateTime.now());

        PasswordSetupTokenEntity setupToken = new PasswordSetupTokenEntity();
        setupToken.setToken(generateToken());
        setupToken.setUser(user);
        setupToken.setExpiryDate(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        setupToken.setUsed(false);

        passwordSetupTokenRepository.save(setupToken);

        String email = user.getEmail();
        String name = user.getName();
        String token = setupToken.getToken();

        // Só envia depois do commit: se a criação do usuário falhar mais adiante, o
        // token não existe no banco e o link chegaria morto — deixando a pessoa sem
        // nenhuma forma de entrar no sistema.
        sendAfterCommit(() -> emailService.sendPasswordSetupLink(email, name, token, TOKEN_VALIDITY_HOURS, welcome));

        log.info("[PASSWORD-SETUP] Link de definição de senha emitido para userId={} (welcome={}), validade={}h",
                user.getId(), welcome, TOKEN_VALIDITY_HOURS);
    }

    private void sendAfterCommit(Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    /**
     * Valida o token da URL antes de a tela pedir a nova senha, para o usuário
     * descobrir que o link expirou sem digitar a senha duas vezes à toa.
     */
    @Transactional(readOnly = true)
    public PasswordSetupInfoDTO validateToken(String token) {
        PasswordSetupTokenEntity setupToken = loadUsableToken(token);
        UserEntity user = setupToken.getUser();
        return new PasswordSetupInfoDTO(user.getName(), user.getEmail());
    }

    /**
     * Define a senha escolhida pelo usuário, consome o token e encerra qualquer
     * sessão anterior.
     */
    @Transactional
    public void completeSetup(CompletePasswordSetupDTO request) {
        PasswordSetupTokenEntity setupToken = loadUsableToken(request.getToken());
        UserEntity user = setupToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setIsFirstAccess(false);
        user.setLastToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        setupToken.setUsed(true);
        setupToken.setUsedAt(LocalDateTime.now());
        passwordSetupTokenRepository.save(setupToken);

        passwordSetupTokenRepository.invalidatePendingTokens(user.getId(), LocalDateTime.now());

        log.info("[PASSWORD-SETUP] Senha de primeiro acesso definida pelo usuário userId={}", user.getId());
    }

    private PasswordSetupTokenEntity loadUsableToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidInputException("Token de definição de senha é obrigatório!");
        }

        PasswordSetupTokenEntity setupToken = passwordSetupTokenRepository.findByTokenWithUser(token)
                .orElseThrow(() -> new InvalidTokenException(
                        "Link de definição de senha inválido. Solicite um novo link ao administrador ou use 'Esqueci minha senha'."));

        if (setupToken.isUsed()) {
            throw new InvalidTokenException(
                    "Este link já foi utilizado. Se você não reconhece esse acesso, use 'Esqueci minha senha' ou contate o administrador.");
        }

        if (setupToken.isExpired()) {
            throw new TokenExpiredException(
                    "Link de definição de senha expirado. Use 'Esqueci minha senha' ou solicite um novo link ao administrador.");
        }

        UserEntity user = setupToken.getUser();
        if (user.getStatus() != null && user.getStatus().getStatus() == StatusEnum.DISABLED) {
            throw new InvalidTokenException("Esta conta está desativada. Contate o administrador do sistema.");
        }

        return setupToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
