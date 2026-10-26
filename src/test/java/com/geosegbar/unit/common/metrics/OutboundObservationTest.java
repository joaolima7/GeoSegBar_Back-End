package com.geosegbar.unit.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.infra.audit.services.AuditService;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import jakarta.mail.internet.MimeMessage;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * As chamadas de saída para o S3 e o SMTP viram métrica (geosegbar_s3_seconds e
 * geosegbar_email_seconds) via Observation. O que se trava aqui: o nome, as tags
 * {operation|template} e error, e que a observação NÃO muda o que o chamador vê
 * — exceção sobe igual onde subia, e engolida continua engolida.
 */
@Tag("unit")
@DisplayName("Unit Tests - Observações de S3 e e-mail")
class OutboundObservationTest extends BaseUnitTest {

    private static final String BUCKET = "bkt-teste";
    private static final String FILE_URL = "https://" + BUCKET + ".s3.amazonaws.com/pasta/arquivo.pdf";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MimeMessage mimeMessage;

    private SimpleMeterRegistry meters;
    private FileStorageService storage;
    private EmailService email;

    @BeforeEach
    void setUp() {
        meters = new SimpleMeterRegistry();
        ObservationRegistry observations = ObservationRegistry.create();
        observations.observationConfig().observationHandler(new DefaultMeterObservationHandler(meters));

        storage = new FileStorageService(s3Client, s3Presigner, observations);
        ReflectionTestUtils.setField(storage, "bucketName", BUCKET);

        email = new EmailService(mailSender, templateEngine, userRepository, auditService, observations);
        ReflectionTestUtils.setField(email, "fromEmail", "noreply@geosegbar.com");
    }

    private long timerCount(String name, String... tags) {
        var timer = meters.find(name).tags(tags).timer();
        return timer == null ? 0 : timer.count();
    }

    // =========================================================================
    // S3
    // =========================================================================
    @Test
    @DisplayName("deleteFile bem-sucedido gera geosegbar.s3{operation=deleteObject,error=none}")
    void deleteComSucesso() {
        storage.deleteFile(FILE_URL);

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        assertThat(timerCount("geosegbar.s3", "operation", "deleteObject", "error", "none")).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteFile com erro do S3 segue engolindo a exceção, mas a observação registra o erro")
    void deleteComErroContinuaEngolido() {
        doThrow(S3Exception.builder().statusCode(500).message("indisponível").build())
                .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertThatCode(() -> storage.deleteFile(FILE_URL)).doesNotThrowAnyException();

        assertThat(timerCount("geosegbar.s3", "operation", "deleteObject", "error", "S3Exception")).isEqualTo(1);
        assertThat(timerCount("geosegbar.s3", "operation", "deleteObject", "error", "none")).isZero();
    }

    @Test
    @DisplayName("objectExists devolve false no 404 e a observação não conta o 404 como erro")
    void headNoVeloDoisNaoEErro() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).build());

        assertThat(storage.objectExists("pasta/arquivo.pdf")).isFalse();

        assertThat(timerCount("geosegbar.s3", "operation", "headObject", "error", "none")).isEqualTo(1);
    }

    @Test
    @DisplayName("objectExists segue propagando erro de infraestrutura e a observação registra o erro")
    void headComErroDeInfraPropaga() {
        S3Exception falha = (S3Exception) S3Exception.builder().statusCode(500).message("fora do ar").build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(falha);

        assertThatThrownBy(() -> storage.objectExists("pasta/arquivo.pdf")).isSameAs(falha);

        assertThat(timerCount("geosegbar.s3", "operation", "headObject", "error", "S3Exception")).isEqualTo(1);
    }

    // =========================================================================
    // E-mail
    // =========================================================================
    @Test
    @DisplayName("Envio bem-sucedido gera geosegbar.email{template=verification_code,error=none}")
    void emailComSucesso() {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        email.sendVerificationCode("user@example.com", "123456");

        verify(mailSender).send(mimeMessage);
        assertThat(timerCount("geosegbar.email", "template", "verification_code", "error", "none")).isEqualTo(1);
    }

    @Test
    @DisplayName("Falha de SMTP segue engolida e auditada, mas a observação registra o erro")
    void emailComFalhaContinuaEngolido() {
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html/>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp fora")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> email.sendPasswordResetCode("user@example.com", "123456"))
                .doesNotThrowAnyException();

        assertThat(timerCount("geosegbar.email", "template", "password_reset_code", "error", "MailSendException"))
                .isEqualTo(1);
        verify(auditService).recordJobError(anyString(), anyString(), any(), anyString(), any(), any(), any());
    }
}
