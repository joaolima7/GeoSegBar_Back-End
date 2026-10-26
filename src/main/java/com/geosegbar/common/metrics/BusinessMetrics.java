package com.geosegbar.common.metrics;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Métricas de negócio expostas ao Prometheus (painéis e alertas do Grafana).
 *
 * <p>
 * Os nomes abaixo são contrato: os painéis e as regras de alerta usam o nome
 * Prometheus exato (o Micrometer troca ponto por sublinhado e acrescenta
 * {@code _total} aos contadores). Não renomear sem mexer neles.
 *
 * <p>
 * <b>Todos os contadores são registrados no construtor, com valor 0.</b> As
 * regras de alerta usam {@code increase()}, que não enxerga o salto 0 → 1 de
 * uma série que ainda não existia: sem o pré-registro, o primeiro erro depois
 * do deploy passaria em branco.
 *
 * <p>
 * <b>Cardinalidade:</b> as tags só recebem os valores fixos dos enums abaixo.
 * Nunca colocar userId, damId, clientId, e-mail, IP, código de estação ou texto
 * livre numa tag — cada valor novo vira uma série nova no Prometheus.
 */
@Component
@Slf4j
public class BusinessMetrics {

    /**
     * Resultado de uma tentativa de login (inclui a renovação de sessão).
     */
    public enum LoginResult {
        SUCCESS("success"),
        MFA_REQUIRED("mfa_required"),
        INVALID_CREDENTIALS("invalid_credentials"),
        ACCOUNT_UNAVAILABLE("account_unavailable"),
        ERROR("error");

        private final String tag;

        LoginResult(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    /**
     * Resultado da verificação do código MFA enviado por e-mail.
     */
    public enum MfaResult {
        SUCCESS("success"),
        INVALID_CODE("invalid_code"),
        EXPIRED_CODE("expired_code"),
        ERROR("error");

        private final String tag;

        MfaResult(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    /**
     * Etapa do fluxo de "esqueci minha senha".
     */
    public enum PasswordResetStep {
        REQUESTED("requested"),
        CODE_VERIFIED("code_verified"),
        COMPLETED("completed"),
        FAILED("failed");

        private final String tag;

        PasswordResetStep(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    /**
     * Tipo de upload direto ao S3 por URL pré-assinada.
     */
    public enum UploadKind {
        PSB("psb"),
        CHECKLIST_PHOTO("checklist_photo");

        private final String tag;

        UploadKind(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    /**
     * Desfecho de um upload pré-assinado.
     */
    public enum UploadResult {
        SUCCESS("success"),
        FAILURE("failure"),
        EXPIRED("expired");

        private final String tag;

        UploadResult(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }
    }

    private static final String RESULT = "result";

    private final Map<LoginResult, Counter> login = new EnumMap<>(LoginResult.class);
    private final Map<MfaResult, Counter> mfa = new EnumMap<>(MfaResult.class);
    private final Map<PasswordResetStep, Counter> passwordReset = new EnumMap<>(PasswordResetStep.class);
    private final Counter bypassKeyAccepted;
    private final Counter bypassKeyRejected;
    private final Counter anaRunSuccess;
    private final Counter anaRunFailure;
    private final Counter anaStationSuccess;
    private final Counter anaStationFailure;
    private final Map<UploadKind, Counter> uploadPresigned = new EnumMap<>(UploadKind.class);
    private final Map<UploadKind, Map<UploadResult, Counter>> uploadCompleted = new EnumMap<>(UploadKind.class);
    private final Counter checklistSubmitted;
    private final Counter readingsCreated;
    private final Counter sessionRefreshSuccess;
    private final Counter sessionRefreshFailure;

    /**
     * Epoch (segundos) da última coleta ANA concluída; 0 = nenhuma desde o boot.
     */
    private final AtomicLong anaLastSuccessEpochSeconds = new AtomicLong(0);

    /**
     * Tamanho da fila de jobs históricos, gravado pelo scheduler. O gauge só lê
     * este valor: nenhuma consulta acontece no scrape.
     */
    private final AtomicLong historicalJobsQueueSize = new AtomicLong(0);

    private final MeterRegistry registry;

    /**
     * Contadores por job (task = nome da ação de auditoria do job). Criados na
     * primeira execução de cada job, já com as duas séries (success e failure),
     * para o increase() do alerta enxergar a primeira falha.
     */
    private final Map<String, Counter[]> jobRuns = new ConcurrentHashMap<>();

    /** Nome de job vindo de constante do código; qualquer outra coisa vira "other". */
    private static final Pattern VALID_TASK = Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (LoginResult result : LoginResult.values()) {
            login.put(result, Counter.builder("geosegbar.auth.login")
                    .description("Tentativas de login, por resultado")
                    .tag(RESULT, result.tag())
                    .register(registry));
        }

        for (MfaResult result : MfaResult.values()) {
            mfa.put(result, Counter.builder("geosegbar.auth.mfa")
                    .description("Verificações do código MFA, por resultado")
                    .tag(RESULT, result.tag())
                    .register(registry));
        }

        for (PasswordResetStep step : PasswordResetStep.values()) {
            passwordReset.put(step, Counter.builder("geosegbar.auth.password.reset")
                    .description("Etapas do fluxo de redefinição de senha")
                    .tag("step", step.tag())
                    .register(registry));
        }

        bypassKeyAccepted = bypassKeyCounter(registry, "accepted");
        bypassKeyRejected = bypassKeyCounter(registry, "rejected");

        anaRunSuccess = anaRunCounter(registry, "success");
        anaRunFailure = anaRunCounter(registry, "failure");

        Gauge.builder("geosegbar.ana.collection.last.success.timestamp", anaLastSuccessEpochSeconds,
                last -> last.get() == 0 ? Double.NaN : last.doubleValue())
                .description("Instante (epoch) da última coleta ANA concluída; NaN até a primeira desde o boot")
                .baseUnit("seconds")
                .register(registry);

        anaStationSuccess = anaStationCounter(registry, "success");
        anaStationFailure = anaStationCounter(registry, "failure");

        Gauge.builder("geosegbar.historical.jobs.queue.size", historicalJobsQueueSize, AtomicLong::doubleValue)
                .description("Jobs de coleta histórica aguardando na fila (valor da última passada do scheduler)")
                .register(registry);

        for (UploadKind kind : UploadKind.values()) {
            uploadPresigned.put(kind, Counter.builder("geosegbar.upload.presigned")
                    .description("URLs de upload pré-assinadas emitidas")
                    .tag("kind", kind.tag())
                    .register(registry));

            Map<UploadResult, Counter> byResult = new EnumMap<>(UploadResult.class);
            for (UploadResult result : UploadResult.values()) {
                byResult.put(result, Counter.builder("geosegbar.upload.completed")
                        .description("Desfecho dos uploads pré-assinados")
                        .tag("kind", kind.tag())
                        .tag(RESULT, result.tag())
                        .register(registry));
            }
            uploadCompleted.put(kind, byResult);
        }

        checklistSubmitted = Counter.builder("geosegbar.checklist.submitted")
                .description("Checklists de inspeção gravados com sucesso")
                .register(registry);

        // "recorded", não "created": o cliente Prometheus 1.x trata "_created"
        // como sufixo reservado (carimbo de criação do contador no OpenMetrics) e
        // o remove do nome — geosegbar.readings.created sairia como
        // geosegbar_readings_total. Nenhum nome de métrica aqui pode terminar
        // em ".created".
        readingsCreated = Counter.builder("geosegbar.readings.recorded")
                .description("Leituras de instrumentos gravadas")
                .register(registry);

        sessionRefreshSuccess = sessionRefreshCounter(registry, "success");
        sessionRefreshFailure = sessionRefreshCounter(registry, "failure");
    }

    /**
     * Renovação de sessão (POST /user/refresh-token) fica fora do contador de
     * login: token vencido ou substituído é rotina do app, não senha errada, e
     * misturar os dois dispararia o alerta de força bruta à toa.
     */
    private static Counter sessionRefreshCounter(MeterRegistry registry, String result) {
        return Counter.builder("geosegbar.auth.session.refresh")
                .description("Renovações de sessão, por resultado")
                .tag(RESULT, result)
                .register(registry);
    }

    private static Counter bypassKeyCounter(MeterRegistry registry, String result) {
        return Counter.builder("geosegbar.auth.bypass.key")
                .description("Uso da chave mestra no reset de senha, por resultado")
                .tag(RESULT, result)
                .register(registry);
    }

    private static Counter anaRunCounter(MeterRegistry registry, String result) {
        return Counter.builder("geosegbar.ana.collection.runs")
                .description("Execuções do job diário de coleta ANA, por resultado")
                .tag(RESULT, result)
                .register(registry);
    }

    private static Counter anaStationCounter(MeterRegistry registry, String result) {
        return Counter.builder("geosegbar.ana.station.collections")
                .description("Coletas por estação ANA, por resultado")
                .tag(RESULT, result)
                .register(registry);
    }

    // =========================================================================
    // Autenticação
    // =========================================================================
    public void loginAttempt(LoginResult result) {
        login.get(result).increment();
    }

    public void sessionRefresh(boolean success) {
        (success ? sessionRefreshSuccess : sessionRefreshFailure).increment();
    }

    public void mfaVerification(MfaResult result) {
        mfa.get(result).increment();
    }

    public void passwordReset(PasswordResetStep step) {
        passwordReset.get(step).increment();
    }

    /**
     * Versão sem o usuário-alvo; prefira {@link #bypassKeyUsed(boolean, Long)}.
     */
    public void bypassKeyUsed(boolean accepted) {
        bypassKeyUsed(accepted, null);
    }

    /**
     * Conta o uso da chave mestra no reset de senha e deixa um WARN para o
     * alerta do Loki (casa pelo trecho {@code [SECURITY] bypassKey}).
     *
     * <p>
     * O log leva só o id do usuário-alvo: nunca o e-mail nem o valor da chave.
     */
    public void bypassKeyUsed(boolean accepted, Long targetUserId) {
        (accepted ? bypassKeyAccepted : bypassKeyRejected).increment();
        log.warn("[SECURITY] bypassKey usado no reset de senha (aceito={}, userId={})", accepted, targetUserId);
    }

    // =========================================================================
    // Coleta ANA
    // =========================================================================
    /**
     * Uma chamada por execução do job. Em caso de sucesso, também atualiza o
     * instante da última coleta concluída.
     */
    public void anaCollectionRun(boolean success) {
        if (success) {
            anaRunSuccess.increment();
            anaLastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
        } else {
            anaRunFailure.increment();
        }
    }

    /**
     * Uma chamada por tentativa de coleta de estação (instrumento).
     */
    public void anaStationCollected(boolean success) {
        (success ? anaStationSuccess : anaStationFailure).increment();
    }

    public void historicalJobsQueueSize(long size) {
        historicalJobsQueueSize.set(size);
    }

    // =========================================================================
    // Jobs agendados
    // =========================================================================
    /**
     * Desfecho de um job: geosegbar_job_runs_total{task, result}.
     *
     * Os jobs capturam a própria exceção e registram o erro na auditoria, então a
     * métrica automática do Spring (tasks_scheduled_execution, outcome=ERROR)
     * nunca vê a falha. Este contador é alimentado pelo AuditService, onde todo
     * job já passa. O rótulo se chama "task" e não "job": "job" é o rótulo do
     * alvo no Prometheus e seria renomeado para exported_job.
     */
    public void jobRun(String task, boolean success) {
        String nome = task != null && VALID_TASK.matcher(task).matches() ? task : "other";
        Counter[] counters = jobRuns.computeIfAbsent(nome, n -> new Counter[]{
            jobRunCounter(n, "success"),
            jobRunCounter(n, "failure")
        });
        counters[success ? 0 : 1].increment();
    }

    private Counter jobRunCounter(String task, String result) {
        return Counter.builder("geosegbar.job.runs")
                .description("Execuções de jobs agendados e tarefas assíncronas, por resultado")
                .tag("task", task)
                .tag(RESULT, result)
                .register(registry);
    }

    // =========================================================================
    // Uploads pré-assinados
    // =========================================================================
    public void uploadPresigned(UploadKind kind) {
        uploadPresigned.get(kind).increment();
    }

    public void uploadCompleted(UploadKind kind, UploadResult result) {
        uploadCompleted.get(kind).get(result).increment();
    }

    /**
     * Para os varredores que expiram vários uploads de uma vez.
     */
    public void uploadCompleted(UploadKind kind, UploadResult result, long count) {
        if (count > 0) {
            uploadCompleted.get(kind).get(result).increment(count);
        }
    }

    // =========================================================================
    // Negócio
    // =========================================================================
    public void checklistSubmitted() {
        checklistSubmitted.increment();
    }

    public void readingsCreated(long count) {
        if (count > 0) {
            readingsCreated.increment(count);
        }
    }
}
