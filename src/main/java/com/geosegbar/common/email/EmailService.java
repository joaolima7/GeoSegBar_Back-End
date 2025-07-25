package com.geosegbar.common.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${application.frontend-url:https://geometrisa-prod.com.br}")
    private String frontendUrl;

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
    public void sendShareFolderEmail(String to, String sharedByName, String folderName, String token) {
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

            String content = templateEngine.process("emails/share-folder", context);
            helper.setText(content, true);

            mailSender.send(mimeMessage);
            log.info("Email de compartilhamento enviado para: {}", to);
        } catch (MessagingException e) {
            log.error("Erro ao enviar email de compartilhamento: {}", e.getMessage());
        }
    }
}
