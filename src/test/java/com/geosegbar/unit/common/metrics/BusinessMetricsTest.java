package com.geosegbar.unit.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import com.geosegbar.common.metrics.BusinessMetrics;
import com.geosegbar.common.metrics.BusinessMetrics.LoginResult;
import com.geosegbar.common.metrics.BusinessMetrics.MfaResult;
import com.geosegbar.common.metrics.BusinessMetrics.PasswordResetStep;
import com.geosegbar.common.metrics.BusinessMetrics.UploadKind;
import com.geosegbar.common.metrics.BusinessMetrics.UploadResult;
import com.geosegbar.config.BaseUnitTest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Trava o contrato das métricas de negócio.
 *
 * Os painéis e as regras de alerta do Grafana usam os nomes exatos; o que
 * quebra aqui quebraria o alerta em silêncio. O ponto mais sutil é o
 * pré-registro: alertas com increase() não enxergam o salto 0 → 1 de uma série
 * que ainda não existia, então toda combinação de tags precisa existir (com
 * valor 0) antes do primeiro evento.
 */
@Tag("unit")
@DisplayName("Unit Tests - BusinessMetrics")
class BusinessMetricsTest extends BaseUnitTest {

    private SimpleMeterRegistry registry;
    private BusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new BusinessMetrics(registry);
    }

    private double count(String name, String... tags) {
        Counter counter = registry.find(name).tags(tags).counter();
        assertThat(counter).as("série %s %s deve existir", name, List.of(tags)).isNotNull();
        return counter.count();
    }

    private double gauge(String name) {
        return registry.get(name).gauge().value();
    }

    // =========================================================================
    // Pré-registro
    // =========================================================================
    @Test
    @DisplayName("Toda combinação de tags existe com valor 0 logo após a construção")
    void todasAsSeriesExistemZeradas() {
        List<String[]> series = List.of(
                new String[]{"geosegbar.auth.login", "result", "success"},
                new String[]{"geosegbar.auth.login", "result", "mfa_required"},
                new String[]{"geosegbar.auth.login", "result", "invalid_credentials"},
                new String[]{"geosegbar.auth.login", "result", "account_unavailable"},
                new String[]{"geosegbar.auth.login", "result", "error"},
                new String[]{"geosegbar.auth.mfa", "result", "success"},
                new String[]{"geosegbar.auth.mfa", "result", "invalid_code"},
                new String[]{"geosegbar.auth.mfa", "result", "expired_code"},
                new String[]{"geosegbar.auth.mfa", "result", "error"},
                new String[]{"geosegbar.auth.password.reset", "step", "requested"},
                new String[]{"geosegbar.auth.password.reset", "step", "code_verified"},
                new String[]{"geosegbar.auth.password.reset", "step", "completed"},
                new String[]{"geosegbar.auth.password.reset", "step", "failed"},
                new String[]{"geosegbar.auth.bypass.key", "result", "accepted"},
                new String[]{"geosegbar.auth.bypass.key", "result", "rejected"},
                new String[]{"geosegbar.ana.collection.runs", "result", "success"},
                new String[]{"geosegbar.ana.collection.runs", "result", "failure"},
                new String[]{"geosegbar.ana.station.collections", "result", "success"},
                new String[]{"geosegbar.ana.station.collections", "result", "failure"},
                new String[]{"geosegbar.upload.presigned", "kind", "psb"},
                new String[]{"geosegbar.upload.presigned", "kind", "checklist_photo"},
                new String[]{"geosegbar.upload.completed", "kind", "psb", "result", "success"},
                new String[]{"geosegbar.upload.completed", "kind", "psb", "result", "failure"},
                new String[]{"geosegbar.upload.completed", "kind", "psb", "result", "expired"},
                new String[]{"geosegbar.upload.completed", "kind", "checklist_photo", "result", "success"},
                new String[]{"geosegbar.upload.completed", "kind", "checklist_photo", "result", "failure"},
                new String[]{"geosegbar.upload.completed", "kind", "checklist_photo", "result", "expired"},
                new String[]{"geosegbar.checklist.submitted"},
                new String[]{"geosegbar.readings.recorded"},
                new String[]{"geosegbar.auth.session.refresh", "result", "success"},
                new String[]{"geosegbar.auth.session.refresh", "result", "failure"});

        for (String[] serie : series) {
            String[] tags = java.util.Arrays.copyOfRange(serie, 1, serie.length);
            assertThat(count(serie[0], tags)).as("%s %s", serie[0], List.of(tags)).isZero();
        }
    }

    @Test
    @DisplayName("Não existe série além das combinações do contrato (cardinalidade fechada)")
    void naoHaSerieExtra() {
        assertThat(registry.find("geosegbar.auth.login").counters()).hasSize(5);
        assertThat(registry.find("geosegbar.auth.mfa").counters()).hasSize(4);
        assertThat(registry.find("geosegbar.auth.password.reset").counters()).hasSize(4);
        assertThat(registry.find("geosegbar.auth.bypass.key").counters()).hasSize(2);
        assertThat(registry.find("geosegbar.ana.collection.runs").counters()).hasSize(2);
        assertThat(registry.find("geosegbar.ana.station.collections").counters()).hasSize(2);
        assertThat(registry.find("geosegbar.upload.presigned").counters()).hasSize(2);
        assertThat(registry.find("geosegbar.upload.completed").counters()).hasSize(6);
        assertThat(registry.find("geosegbar.checklist.submitted").counters()).hasSize(1);
        assertThat(registry.find("geosegbar.readings.recorded").counters()).hasSize(1);
        assertThat(registry.find("geosegbar.auth.session.refresh").counters()).hasSize(2);
    }

    // =========================================================================
    // Autenticação
    // =========================================================================
    @ParameterizedTest(name = "login {0}")
    @EnumSource(LoginResult.class)
    @DisplayName("loginAttempt incrementa só a série do resultado informado")
    void loginIncrementaSoOResultado(LoginResult result) {
        metrics.loginAttempt(result);

        for (LoginResult other : LoginResult.values()) {
            assertThat(count("geosegbar.auth.login", "result", other.tag()))
                    .as(other.tag()).isEqualTo(other == result ? 1.0 : 0.0);
        }
    }

    @ParameterizedTest(name = "mfa {0}")
    @EnumSource(MfaResult.class)
    @DisplayName("mfaVerification incrementa só a série do resultado informado")
    void mfaIncrementaSoOResultado(MfaResult result) {
        metrics.mfaVerification(result);

        for (MfaResult other : MfaResult.values()) {
            assertThat(count("geosegbar.auth.mfa", "result", other.tag()))
                    .as(other.tag()).isEqualTo(other == result ? 1.0 : 0.0);
        }
    }

    @ParameterizedTest(name = "reset de senha {0}")
    @EnumSource(PasswordResetStep.class)
    @DisplayName("passwordReset incrementa só a série da etapa informada")
    void resetIncrementaSoAEtapa(PasswordResetStep step) {
        metrics.passwordReset(step);

        for (PasswordResetStep other : PasswordResetStep.values()) {
            assertThat(count("geosegbar.auth.password.reset", "step", other.tag()))
                    .as(other.tag()).isEqualTo(other == step ? 1.0 : 0.0);
        }
    }

    @Test
    @DisplayName("bypassKeyUsed conta aceita e rejeitada em séries separadas")
    void bypassKeyAceitaERejeitada() {
        metrics.bypassKeyUsed(true, 10L);
        metrics.bypassKeyUsed(false, 11L);
        metrics.bypassKeyUsed(false, 12L);

        assertThat(count("geosegbar.auth.bypass.key", "result", "accepted")).isEqualTo(1.0);
        assertThat(count("geosegbar.auth.bypass.key", "result", "rejected")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("bypassKeyUsed(boolean) mantém o contrato de contagem")
    void bypassKeySemUsuario() {
        metrics.bypassKeyUsed(true);
        metrics.bypassKeyUsed(false);

        assertThat(count("geosegbar.auth.bypass.key", "result", "accepted")).isEqualTo(1.0);
        assertThat(count("geosegbar.auth.bypass.key", "result", "rejected")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("bypassKeyUsed registra WARN com o marcador do alerta e só o id do usuário")
    void bypassKeyLogaWarnParaOAlerta() {
        Logger logger = (Logger) LoggerFactory.getLogger(BusinessMetrics.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            metrics.bypassKeyUsed(true, 42L);
            metrics.bypassKeyUsed(false, 7L);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            // O alerta do Loki casa por este trecho exato
            assertThat(event.getFormattedMessage()).contains("[SECURITY] bypassKey");
        });
        assertThat(appender.list.get(0).getFormattedMessage()).contains("aceito=true").contains("userId=42");
        assertThat(appender.list.get(1).getFormattedMessage()).contains("aceito=false").contains("userId=7");
    }

    // =========================================================================
    // Coleta ANA
    // =========================================================================
    @Test
    @DisplayName("Gauge da última coleta ANA é NaN até o primeiro sucesso")
    void gaugeAnaNaNAntesDoPrimeiroSucesso() {
        assertThat(gauge("geosegbar.ana.collection.last.success.timestamp")).isNaN();

        metrics.anaCollectionRun(false);

        assertThat(gauge("geosegbar.ana.collection.last.success.timestamp")).isNaN();
        assertThat(count("geosegbar.ana.collection.runs", "result", "failure")).isEqualTo(1.0);
        assertThat(count("geosegbar.ana.collection.runs", "result", "success")).isZero();
    }

    @Test
    @DisplayName("Sucesso da coleta ANA conta a execução e leva o gauge para agora (em segundos)")
    void sucessoDaColetaAtualizaOGauge() {
        long antes = Instant.now().getEpochSecond();

        metrics.anaCollectionRun(true);

        long depois = Instant.now().getEpochSecond();
        assertThat(count("geosegbar.ana.collection.runs", "result", "success")).isEqualTo(1.0);
        assertThat(count("geosegbar.ana.collection.runs", "result", "failure")).isZero();
        assertThat(gauge("geosegbar.ana.collection.last.success.timestamp"))
                .isBetween((double) antes, (double) depois);
        assertThat(registry.get("geosegbar.ana.collection.last.success.timestamp").gauge().getId().getBaseUnit())
                .isEqualTo("seconds");
    }

    @Test
    @DisplayName("Falha depois de um sucesso não mexe no gauge da última coleta")
    void falhaNaoMexeNoGauge() {
        metrics.anaCollectionRun(true);
        double ultimoSucesso = gauge("geosegbar.ana.collection.last.success.timestamp");

        metrics.anaCollectionRun(false);

        assertThat(gauge("geosegbar.ana.collection.last.success.timestamp")).isEqualTo(ultimoSucesso);
    }

    @Test
    @DisplayName("anaStationCollected conta sucesso e falha por estação")
    void coletaPorEstacao() {
        metrics.anaStationCollected(true);
        metrics.anaStationCollected(true);
        metrics.anaStationCollected(false);

        assertThat(count("geosegbar.ana.station.collections", "result", "success")).isEqualTo(2.0);
        assertThat(count("geosegbar.ana.station.collections", "result", "failure")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Gauge da fila de jobs históricos começa em 0 e reflete o último valor informado")
    void gaugeDaFilaRefleteOUltimoValor() {
        assertThat(gauge("geosegbar.historical.jobs.queue.size")).isZero();

        metrics.historicalJobsQueueSize(12);
        assertThat(gauge("geosegbar.historical.jobs.queue.size")).isEqualTo(12.0);

        metrics.historicalJobsQueueSize(3);
        assertThat(gauge("geosegbar.historical.jobs.queue.size")).isEqualTo(3.0);

        metrics.historicalJobsQueueSize(0);
        assertThat(gauge("geosegbar.historical.jobs.queue.size")).isZero();
    }

    // =========================================================================
    // Uploads
    // =========================================================================
    @ParameterizedTest(name = "presign {0}")
    @EnumSource(UploadKind.class)
    @DisplayName("uploadPresigned incrementa só o tipo informado")
    void presignPorTipo(UploadKind kind) {
        metrics.uploadPresigned(kind);

        for (UploadKind other : UploadKind.values()) {
            assertThat(count("geosegbar.upload.presigned", "kind", other.tag()))
                    .as(other.tag()).isEqualTo(other == kind ? 1.0 : 0.0);
        }
    }

    @ParameterizedTest(name = "completed {0}")
    @EnumSource(UploadKind.class)
    @DisplayName("uploadCompleted incrementa só a combinação tipo + resultado")
    void completedPorTipoEResultado(UploadKind kind) {
        for (UploadResult result : UploadResult.values()) {
            metrics.uploadCompleted(kind, result);

            for (UploadKind otherKind : UploadKind.values()) {
                for (UploadResult otherResult : UploadResult.values()) {
                    boolean visitada = otherKind == kind && otherResult.ordinal() <= result.ordinal();
                    assertThat(count("geosegbar.upload.completed", "kind", otherKind.tag(), "result", otherResult.tag()))
                            .as("%s/%s após %s", otherKind, otherResult, result)
                            .isEqualTo(visitada ? 1.0 : 0.0);
                }
            }
        }
    }

    @Test
    @DisplayName("uploadCompleted com quantidade soma o lote; zero e negativo não fazem nada")
    void completedEmLote() {
        metrics.uploadCompleted(UploadKind.PSB, UploadResult.EXPIRED, 4);
        metrics.uploadCompleted(UploadKind.PSB, UploadResult.EXPIRED, 0);
        metrics.uploadCompleted(UploadKind.PSB, UploadResult.EXPIRED, -2);

        assertThat(count("geosegbar.upload.completed", "kind", "psb", "result", "expired")).isEqualTo(4.0);
        assertThat(count("geosegbar.upload.completed", "kind", "checklist_photo", "result", "expired")).isZero();
    }

    // =========================================================================
    // Negócio
    // =========================================================================
    @Test
    @DisplayName("checklistSubmitted soma 1 a cada chamada")
    void checklistSubmetido() {
        metrics.checklistSubmitted();
        metrics.checklistSubmitted();

        assertThat(count("geosegbar.checklist.submitted")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("jobRun cria as duas séries do job na primeira execução e conta cada desfecho")
    void jobRunPreRegistraFalha() {
        metrics.jobRun("VERIFICATION_CODE_CLEANUP", true);

        assertThat(count("geosegbar.job.runs", "task", "VERIFICATION_CODE_CLEANUP", "result", "success")).isEqualTo(1.0);
        // A série de falha já existe, em zero: o increase() do alerta vê a primeira falha.
        assertThat(count("geosegbar.job.runs", "task", "VERIFICATION_CODE_CLEANUP", "result", "failure")).isZero();

        metrics.jobRun("VERIFICATION_CODE_CLEANUP", false);
        assertThat(count("geosegbar.job.runs", "task", "VERIFICATION_CODE_CLEANUP", "result", "failure")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("jobRun não aceita nome livre como tag")
    void jobRunNomeInvalidoViraOther() {
        metrics.jobRun("nome com espaço e {chaves}", false);
        metrics.jobRun(null, true);

        assertThat(count("geosegbar.job.runs", "task", "other", "result", "failure")).isEqualTo(1.0);
        assertThat(count("geosegbar.job.runs", "task", "other", "result", "success")).isEqualTo(1.0);
        assertThat(registry.find("geosegbar.job.runs").counters()).hasSize(2);
    }

    @Test
    @DisplayName("sessionRefresh conta à parte do login")
    void sessionRefreshSeparadoDoLogin() {
        metrics.sessionRefresh(true);
        metrics.sessionRefresh(false);
        metrics.sessionRefresh(false);

        assertThat(count("geosegbar.auth.session.refresh", "result", "success")).isEqualTo(1.0);
        assertThat(count("geosegbar.auth.session.refresh", "result", "failure")).isEqualTo(2.0);
        assertThat(count("geosegbar.auth.login", "result", "invalid_credentials")).isZero();
    }

    @Test
    @DisplayName("readingsCreated soma a quantidade informada")
    void leiturasCriadas() {
        metrics.readingsCreated(1);
        metrics.readingsCreated(40);

        assertThat(count("geosegbar.readings.recorded")).isEqualTo(41.0);
    }

    @Test
    @DisplayName("readingsCreated ignora zero e negativo")
    void leiturasZeroENegativoNaoContam() {
        metrics.readingsCreated(0);
        metrics.readingsCreated(-5);

        assertThat(count("geosegbar.readings.recorded")).isZero();
    }

    // =========================================================================
    // Nomes no Prometheus (o que os painéis e alertas realmente consultam)
    // =========================================================================
    @Test
    @DisplayName("Os nomes expostos ao Prometheus batem com o contrato dos painéis e alertas")
    void nomesNoPrometheus() {
        PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        BusinessMetrics promMetrics = new BusinessMetrics(prometheus);
        promMetrics.loginAttempt(LoginResult.INVALID_CREDENTIALS);
        promMetrics.anaCollectionRun(true);
        promMetrics.historicalJobsQueueSize(5);
        promMetrics.readingsCreated(3);
        promMetrics.sessionRefresh(false);

        String scrape = prometheus.scrape();

        assertThat(scrape).contains(
                "geosegbar_auth_login_total{result=\"invalid_credentials\"} 1.0",
                "geosegbar_auth_login_total{result=\"success\"} 0.0",
                "geosegbar_auth_mfa_total{result=\"expired_code\"} 0.0",
                "geosegbar_auth_password_reset_total{step=\"code_verified\"} 0.0",
                "geosegbar_auth_bypass_key_total{result=\"rejected\"} 0.0",
                "geosegbar_ana_collection_runs_total{result=\"success\"} 1.0",
                "geosegbar_ana_collection_runs_total{result=\"failure\"} 0.0",
                "geosegbar_ana_collection_last_success_timestamp_seconds ",
                "geosegbar_ana_station_collections_total{result=\"failure\"} 0.0",
                "geosegbar_historical_jobs_queue_size 5.0",
                "geosegbar_upload_presigned_total{kind=\"checklist_photo\"} 0.0",
                "geosegbar_upload_completed_total{kind=\"psb\",result=\"expired\"} 0.0",
                "geosegbar_checklist_submitted_total 0.0",
                "geosegbar_readings_recorded_total 3.0",
                "geosegbar_auth_session_refresh_total{result=\"failure\"} 1.0",
                "geosegbar_auth_session_refresh_total{result=\"success\"} 0.0");
        // Sufixo "_created" é reservado no cliente Prometheus 1.x e some do nome:
        // nenhum contador pode terminar em ".created".
        assertThat(scrape).doesNotContain("geosegbar_readings_total ");
    }

    @Test
    @DisplayName("No Prometheus, o gauge da coleta ANA é NaN até o primeiro sucesso")
    void gaugeAnaNaNNoPrometheus() {
        PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new BusinessMetrics(prometheus);

        assertThat(prometheus.scrape())
                .contains("geosegbar_ana_collection_last_success_timestamp_seconds NaN");
    }

    @AfterEach
    void tearDown() {
        registry.close();
    }
}
