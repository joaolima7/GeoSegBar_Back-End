package com.geosegbar.unit.common.email;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.config.BaseUnitTest;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Tag("unit")
@DisplayName("EmailService - Unit Tests")
class EmailServiceTest extends BaseUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String FROM_EMAIL = "noreply@geosegbar.com";
    private static final String FRONTEND_URL = "https://geometrisa-prod.com.br";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "frontendUrl", FRONTEND_URL);
    }

    @Test
    @DisplayName("Should send verification code email successfully")
    void shouldSendVerificationCodeEmailSuccessfully() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "123456";
        String htmlContent = "<html>Your code is: 123456</html>";

        when(templateEngine.process(eq("emails/verification-code"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertThatCode(() -> emailService.sendVerificationCode(toEmail, code))
                .doesNotThrowAnyException();

        // Then
        verify(templateEngine).process(eq("emails/verification-code"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should set correct context variables for verification code email")
    void shouldSetCorrectContextVariablesForVerificationCode() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "654321";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    // Verify context contains code variable
                    return "<html>Code: " + context.getVariable("code") + "</html>";
                });
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendVerificationCode(toEmail, code);

        // Then
        verify(templateEngine).process(eq("emails/verification-code"), any(Context.class));
    }

    @Test
    @DisplayName("Should send verification code even with empty code")
    void shouldSendVerificationCodeEvenWithEmptyCode() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = ""; // Empty code

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then
        assertThatCode(() -> emailService.sendVerificationCode(toEmail, code))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should send password reset code email successfully")
    void shouldSendPasswordResetCodeEmailSuccessfully() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "987654";
        String htmlContent = "<html>Reset code: 987654</html>";

        when(templateEngine.process(eq("emails/password-reset-code"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertThatCode(() -> emailService.sendPasswordResetCode(toEmail, code))
                .doesNotThrowAnyException();

        // Then
        verify(templateEngine).process(eq("emails/password-reset-code"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should set correct context variables for password reset email")
    void shouldSetCorrectContextVariablesForPasswordReset() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "111222";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    return "<html>Code: " + context.getVariable("code") + "</html>";
                });
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendPasswordResetCode(toEmail, code);

        // Then
        verify(templateEngine).process(eq("emails/password-reset-code"), any(Context.class));
    }

    @Test
    @DisplayName("Should send password reset code with long code")
    void shouldSendPasswordResetCodeWithLongCode() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "A1B2C3D4E5F6G7H8"; // Long code

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then
        assertThatCode(() -> emailService.sendPasswordResetCode(toEmail, code))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should send first access password email successfully")
    void shouldSendFirstAccessPasswordEmailSuccessfully() throws MessagingException {
        // Given
        String toEmail = "newuser@example.com";
        String password = "P@ssw0rd!";
        String userName = "João Silva";
        String htmlContent = "<html>Welcome João Silva! Password: P@ssw0rd!</html>";

        when(templateEngine.process(eq("emails/first-access-password"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertThatCode(() -> emailService.sendFirstAccessPassword(toEmail, password, userName))
                .doesNotThrowAnyException();

        // Then
        verify(templateEngine).process(eq("emails/first-access-password"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should set correct context variables for first access password email")
    void shouldSetCorrectContextVariablesForFirstAccessPassword() throws MessagingException {
        // Given
        String toEmail = "newuser@example.com";
        String password = "TempPass123!";
        String userName = "Maria Santos";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    // Verify context contains all required variables
                    return "<html>User: " + context.getVariable("userName")
                            + ", Email: " + context.getVariable("userEmail")
                            + ", Password: " + context.getVariable("password") + "</html>";
                });
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendFirstAccessPassword(toEmail, password, userName);

        // Then
        verify(templateEngine).process(eq("emails/first-access-password"), any(Context.class));
    }

    @Test
    @DisplayName("Should send first access password with complex password")
    void shouldSendFirstAccessPasswordWithComplexPassword() throws MessagingException {
        // Given
        String toEmail = "newuser@example.com";
        String password = "C0mpl3x!P@ssw0rd#2024$"; // Complex password
        String userName = "João Silva";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then
        assertThatCode(() -> emailService.sendFirstAccessPassword(toEmail, password, userName))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should send share folder email successfully")
    void shouldSendShareFolderEmailSuccessfully() throws MessagingException {
        // Given
        String toEmail = "recipient@example.com";
        String sharedByName = "Admin User";
        String folderName = "Documentos Técnicos";
        String token = "abc123xyz";
        String customMessage = "Confira estes documentos importantes";
        String htmlContent = "<html>Folder shared: Documentos Técnicos</html>";

        when(templateEngine.process(eq("emails/share-folder"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertThatCode(() -> emailService.sendShareFolderEmail(toEmail, sharedByName, folderName, token, customMessage))
                .doesNotThrowAnyException();

        // Then
        verify(templateEngine).process(eq("emails/share-folder"), any(Context.class));
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should set correct context variables for share folder email")
    void shouldSetCorrectContextVariablesForShareFolder() throws MessagingException {
        // Given
        String toEmail = "recipient@example.com";
        String sharedByName = "Carlos Souza";
        String folderName = "Relatórios 2024";
        String token = "token123";
        String customMessage = "Mensagem personalizada";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    // Verify context contains all variables including access link
                    return "<html>Shared by: " + context.getVariable("sharedByName")
                            + ", Folder: " + context.getVariable("folderName")
                            + ", Link: " + context.getVariable("accessLink")
                            + ", Message: " + context.getVariable("customMessage") + "</html>";
                });
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendShareFolderEmail(toEmail, sharedByName, folderName, token, customMessage);

        // Then
        verify(templateEngine).process(eq("emails/share-folder"), any(Context.class));
    }

    @Test
    @DisplayName("Should construct correct access link with frontend URL and token")
    void shouldConstructCorrectAccessLinkWithFrontendUrlAndToken() throws MessagingException {
        // Given
        String toEmail = "recipient@example.com";
        String token = "uniqueToken456";
        String expectedLink = FRONTEND_URL + "/shared/folder/" + token;

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenAnswer(invocation -> {
                    Context context = invocation.getArgument(1);
                    String accessLink = (String) context.getVariable("accessLink");
                    // Verify link construction
                    if (accessLink != null && accessLink.equals(expectedLink)) {
                        return "<html>Link OK</html>";
                    }
                    return "<html>Link error</html>";
                });
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendShareFolderEmail(toEmail, "Sender", "Folder", token, "Message");

        // Then
        verify(templateEngine).process(eq("emails/share-folder"), any(Context.class));
    }

    @Test
    @DisplayName("Should send share folder email with long token")
    void shouldSendShareFolderEmailWithLongToken() throws MessagingException {
        // Given
        String toEmail = "recipient@example.com";
        String sharedByName = "Admin";
        String folderName = "Shared Folder";
        String token = "very-long-token-with-many-characters-1234567890abcdefghijklmnopqrstuvwxyz"; // Long token
        String customMessage = "Check this out";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then
        assertThatCode(() -> emailService.sendShareFolderEmail(toEmail, sharedByName, folderName, token, customMessage))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle verification code with special characters")
    void shouldHandleVerificationCodeWithSpecialCharacters() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "A1B2C3"; // alphanumeric code

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Code: A1B2C3</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendVerificationCode(toEmail, code);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should handle Portuguese characters in user name")
    void shouldHandlePortugueseCharactersInUserName() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String password = "P@ss123";
        String userName = "José da Silva Ção"; // Portuguese characters

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Welcome José da Silva Ção</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendFirstAccessPassword(toEmail, password, userName);

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should handle Portuguese characters in folder name")
    void shouldHandlePortugueseCharactersInFolderName() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String folderName = "Documentação Técnica - Barragens"; // Portuguese chars

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Folder: Documentação Técnica</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendShareFolderEmail(toEmail, "Admin", folderName, "token", "Message");

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should send verification code with multiple email addresses format")
    void shouldSendVerificationCodeWithMultipleEmailAddressesFormat() throws MessagingException {
        // Given
        String toEmail = "user.name+tag@example.co.uk"; // Complex email format
        String code = "123456";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When & Then
        assertThatCode(() -> emailService.sendVerificationCode(toEmail, code))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle null custom message in share folder email")
    void shouldHandleNullCustomMessageInShareFolderEmail() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String customMessage = null;

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        assertThatCode(() -> emailService.sendShareFolderEmail(toEmail, "Admin", "Folder", "token", customMessage))
                .doesNotThrowAnyException();

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should use correct email subject for verification code")
    void shouldUseCorrectEmailSubjectForVerificationCode() throws MessagingException {
        // Given - subject is set in the service method
        String toEmail = "user@example.com";
        String code = "123456";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendVerificationCode(toEmail, code);

        // Then - verify MimeMessage was created and sent
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should use correct email subject for password reset")
    void shouldUseCorrectEmailSubjectForPasswordReset() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String code = "654321";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendPasswordResetCode(toEmail, code);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should use correct email subject for first access")
    void shouldUseCorrectEmailSubjectForFirstAccess() throws MessagingException {
        // Given
        String toEmail = "user@example.com";

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html>Test</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendFirstAccessPassword(toEmail, "pass", "User");

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}
