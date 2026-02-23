package com.geosegbar.common.email;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.geosegbar.common.email.dtos.AttachmentData;
import com.geosegbar.common.email.dtos.SupportEmailRequestDTO;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.user.dto.UserSupportInfoProjection;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final int MAX_ATTACHMENTS = 5;
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;

    /**
     * Auto-injeção lazy para permitir que métodos @Async da mesma classe sejam
     * invocados através do proxy do Spring, garantindo o comportamento
     * assíncrono.
     */
    @Lazy
    @Autowired
    private EmailService self;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${application.admin-email:joaocaetanodev@gmail.com}")
    private String adminEmail;

    private String supportEmail = "support@geometrisa-prod.com.br";

    @Value("${application.frontend-url:https://geometrisa-prod.com.br}")
    private String frontendUrl;

    @Async
    public void sendInternalErrorException(String errorMessage, String stackTrace, String userContext, String requestEndpoint, String requestMethod) {
        try {
            Context context = new Context();
            context.setVariable("errorMessage", errorMessage);
            context.setVariable("stackTrace", stackTrace);
            context.setVariable("userContext", userContext);
            context.setVariable("requestEndpoint", requestEndpoint);
            context.setVariable("requestMethod", requestMethod);
            context.setVariable("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            String htmlContent = templateEngine.process("emails/error-report", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("🚨 [GeoSegBar] Erro Interno - " + errorMessage);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Relatório de erro enviado para o administrador: {}", adminEmail);
        } catch (MessagingException e) {
            log.error("FALHA CRÍTICA: Não foi possível enviar o email de relatório de erro: {}", e.getMessage());
        }
    }

    @Async
    public void sendVerificationCode(String toEmail, String code) {
        try {
            Context context = new Context();
            context.setVariable("code", code);

            String htmlContent = templateEngine.process("emails/verification-code", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Código de Verificação - GeoSegBar");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de verificação enviado para: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de verificação: {}", e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            Context context = new Context();
            context.setVariable("code", code);

            String htmlContent = templateEngine.process("emails/password-reset-code", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Redefinição de Senha - GeoSegBar");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de redefinição de senha enviado para: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de redefinição de senha: {}", e.getMessage());
        }
    }

    @Async
    public void sendFirstAccessPassword(String toEmail, String password, String userName) {
        try {
            Context context = new Context();
            context.setVariable("password", password);
            context.setVariable("userName", userName);
            context.setVariable("userEmail", toEmail);

            String htmlContent = templateEngine.process("emails/first-access-password", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Bem-vindo ao GeoSegBar - Sua senha de acesso");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de primeiro acesso enviado para: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de primeiro acesso: {}", e.getMessage());
        }
    }

    @Async
    public void sendShareFolderEmail(String to, String sharedByName, String folderName, String token, String customMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Pasta compartilhada: " + folderName);

            Context context = new Context();
            context.setVariable("sharedByName", sharedByName);
            context.setVariable("folderName", folderName);
            context.setVariable("accessLink", frontendUrl + "/shared/folder/" + token);
            context.setVariable("customMessage", customMessage);

            String content = templateEngine.process("emails/share-folder", context);
            helper.setText(content, true);

            mailSender.send(mimeMessage);
            log.info("Email de compartilhamento enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de compartilhamento: {}", e.getMessage());
        }
    }

    /**
     * Ponto de entrada público e síncrono para solicitações de suporte. Busca
     * os dados do usuário com uma projeção mínima (só os campos do email),
     * valida e lê os bytes dos anexos com a requisição HTTP ainda ativa, depois
     * delega o envio ao método assíncrono via proxy do Spring.
     */
    public void sendSupportRequest(SupportEmailRequestDTO request) {
        UserSupportInfoProjection userInfo = userRepository
                .findSupportInfoById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + request.getUserId()));

        List<AttachmentData> attachments = resolveAttachments(request.getAttachments());

        self.dispatchSupportEmailAsync(
                userInfo.getName(), userInfo.getEmail(), userInfo.getPhone(),
                userInfo.getClientName(), request.getMessage(), attachments);
    }

    @Async
    public void dispatchSupportEmailAsync(String senderName, String senderEmail, String phone,
            String clientName, String message, List<AttachmentData> attachments) {
        try {
            Context context = new Context();
            context.setVariable("senderName", senderName);
            context.setVariable("senderEmail", senderEmail);
            context.setVariable("phone", phone);
            context.setVariable("clientName", clientName);
            context.setVariable("message", message);
            context.setVariable("attachmentCount", attachments != null ? attachments.size() : 0);
            context.setVariable("timestamp",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            String htmlContent = templateEngine.process("emails/support-request", context);

            boolean hasAttachments = attachments != null && !attachments.isEmpty();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachments, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(supportEmail);
            helper.setSubject("📋 [Suporte] " + senderName + " - " + senderEmail);
            helper.setText(htmlContent, true);

            if (hasAttachments) {
                for (AttachmentData attachment : attachments) {
                    helper.addAttachment(
                            attachment.filename(),
                            new ByteArrayResource(attachment.content()),
                            attachment.contentType());
                }
            }

            mailSender.send(mimeMessage);
            log.info("Email de suporte enviado de {} ({}) para o administrador: {}", senderName, senderEmail, adminEmail);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de suporte de {} ({}): {}", senderName, senderEmail, e.getMessage());
        }
    }

    private List<AttachmentData> resolveAttachments(List<org.springframework.web.multipart.MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        if (files.size() > MAX_ATTACHMENTS) {
            throw new InvalidInputException(
                    "Máximo de " + MAX_ATTACHMENTS + " anexos permitido por solicitação.");
        }

        List<AttachmentData> result = new ArrayList<>(files.size());

        for (org.springframework.web.multipart.MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new InvalidInputException(
                        "O arquivo '" + file.getOriginalFilename() + "' excede o limite de 5 MB por anexo.");
            }
            try {
                String filename = file.getOriginalFilename() != null
                        ? file.getOriginalFilename()
                        : "anexo_" + result.size();
                String contentType = file.getContentType() != null
                        ? file.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                result.add(new AttachmentData(filename, file.getBytes(), contentType));
            } catch (IOException e) {
                log.error("Erro ao ler conteúdo do anexo '{}': {}", file.getOriginalFilename(), e.getMessage());
                throw new InvalidInputException("Falha ao processar o anexo: " + file.getOriginalFilename());
            }
        }

        return result;
    }
}
