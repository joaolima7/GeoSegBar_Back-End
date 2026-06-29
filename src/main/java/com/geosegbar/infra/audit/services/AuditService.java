package com.geosegbar.infra.audit.services;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.entities.AuditLogEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.audit.config.AuditProperties;
import com.geosegbar.infra.audit.dtos.AuditLogDetailDTO;
import com.geosegbar.infra.audit.dtos.AuditLogFilterDTO;
import com.geosegbar.infra.audit.dtos.AuditLogSummaryDTO;
import com.geosegbar.infra.audit.events.AuditEvent;
import com.geosegbar.infra.audit.persistence.jpa.AuditLogRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ponto central de registro de auditoria.
 * <p>
 * - O filtro HTTP monta um {@link AuditContext} completo e chama {@link #record}.
 * - Jobs/serviços async usam os atalhos {@code recordJob*} (sem requisição HTTP).
 * <p>
 * O serviço NÃO grava diretamente: monta o {@link AuditLogEntity}, aplica
 * truncamento e publica um {@link AuditEvent} consumido de forma assíncrona.
 * Toda a operação é best-effort — uma falha aqui jamais propaga para o chamador.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private static final String ACTOR_SYSTEM_JOB = "Sistema (Job)";
    private static final String ACTOR_ANONYMOUS = "Anônimo/Não autenticado";

    private final ApplicationEventPublisher eventPublisher;
    private final AuditProperties auditProperties;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Value("${application.system-user-email}")
    private String systemUserEmail;

    /**
     * ID do usuário SISTEMA, resolvido sob demanda e cacheado (não muda em
     * runtime). Usado para vincular jobs/tarefas automáticas ao usuário real.
     */
    private final AtomicReference<Long> systemUserIdCache = new AtomicReference<>();

    /**
     * Registra uma ação a partir de um contexto já montado (usado pelo filtro
     * HTTP e por chamadas programáticas que preenchem tudo).
     */
    public void record(AuditContext ctx) {
        if (!auditProperties.isEnabled() || ctx == null) {
            log.debug("[AUDIT] IGNORADO (enabled={}, ctx={})",
                    auditProperties.isEnabled(), ctx != null);
            return;
        }

        try {
            AuditLogEntity entity = toEntity(ctx);
            log.debug("[AUDIT] >>> PUBLICANDO evento: action={} status={} source={} actorUserId={} actorLabel={} httpStatus={} endpoint={}",
                    entity.getAction(), entity.getStatus(), entity.getSource(),
                    entity.getActorUserId(), entity.getActorLabel(),
                    entity.getHttpStatus(), entity.getEndpoint());
            eventPublisher.publishEvent(new AuditEvent(entity));
        } catch (Exception e) {
            // Auditar nunca pode quebrar o fluxo de origem.
            log.error("[AUDIT] Falha ao montar/registrar auditoria (action={}): {}",
                    ctx.getAction(), e.getMessage(), e);
        }
    }

    // ---- Atalhos para jobs/async (sem SecurityContext) ----

    public void recordJobSuccess(String action, String actionLabel, AuditSource source,
            String message, String traceId, Long durationMs) {
        record(AuditContext.builder()
                .action(action)
                .actionLabel(actionLabel)
                .source(source != null ? source : AuditSource.SCHEDULED)
                .status(AuditStatus.SUCCESS)
                .message(message)
                .traceId(traceId)
                .durationMs(durationMs)
                .build());
    }

    public void recordJobError(String action, String actionLabel, AuditSource source,
            String message, Throwable error, String traceId, Long durationMs) {
        record(AuditContext.builder()
                .action(action)
                .actionLabel(actionLabel)
                .source(source != null ? source : AuditSource.SCHEDULED)
                .status(AuditStatus.ERROR)
                .message(message)
                .error(error)
                .traceId(traceId)
                .durationMs(durationMs)
                .build());
    }

    /**
     * Resolve (e cacheia) o ID do usuário SISTEMA a partir do e-mail configurado.
     * Usado para vincular jobs/tarefas automáticas ao usuário real. Se não for
     * possível resolver, o chamador cai em rótulo textual — nunca quebra.
     */
    private Long resolveSystemUserId() {
        Long cached = systemUserIdCache.get();
        if (cached != null) {
            return cached;
        }
        try {
            Long id = userRepository.findSystemUserIdByEmail(systemUserEmail).orElse(null);
            if (id != null) {
                systemUserIdCache.set(id);
            }
            return id;
        } catch (Exception e) {
            log.warn("[AUDIT] Não foi possível resolver o usuário SISTEMA para auditar job: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gera um identificador curto de correlação para amarrar início/fim de um
     * job ou uma cadeia de operações.
     */
    public String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ---- Consulta (read) ----

    /**
     * Listagem paginada para o usuário comum (campos básicos). Os dados do ator
     * são resolvidos por JOIN com a tabela de usuários (sempre atualizados).
     */
    public Page<AuditLogSummaryDTO> findSummaries(AuditLogFilterDTO f, Pageable pageable) {
        return auditLogRepository.findSummaries(
                f.getStartDate(), f.getEndDate(), f.getActorUserId(),
                likePattern(f.getActorEmail()), likePattern(f.getAction()),
                enumName(f.getStatus()), enumName(f.getSource()), normalize(f.getHttpMethod()),
                f.getEntityType(), f.getEntityId(), pageable)
                .map(AuditLogSummaryDTO::fromProjection);
    }

    /**
     * Listagem paginada para admin/dev (todos os campos).
     */
    public Page<AuditLogDetailDTO> findDetails(AuditLogFilterDTO f, Pageable pageable) {
        return auditLogRepository.findDetails(
                f.getStartDate(), f.getEndDate(), f.getActorUserId(),
                likePattern(f.getActorEmail()), likePattern(f.getAction()),
                enumName(f.getStatus()), enumName(f.getSource()), normalize(f.getHttpMethod()),
                f.getEntityType(), f.getEntityId(), pageable)
                .map(AuditLogDetailDTO::fromProjection);
    }

    /**
     * Detalhe completo de um único registro (admin/dev).
     */
    public AuditLogDetailDTO findDetailById(Long id) {
        return auditLogRepository.findDetailById(id)
                .map(AuditLogDetailDTO::fromProjection)
                .orElseThrow(() -> new NotFoundException("Registro de auditoria não encontrado para id: " + id));
    }

    /**
     * Monta o padrão LIKE em minúsculas ({@code %valor%}) para busca por
     * substring. Retorna null quando o filtro está vazio (filtro ignorado).
     */
    private String likePattern(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return "%" + value.trim().toLowerCase() + "%";
    }

    /**
     * Normaliza o método HTTP (uppercase) para comparação exata; null se vazio.
     */
    private String normalize(String httpMethod) {
        if (httpMethod == null || httpMethod.isBlank()) {
            return null;
        }
        return httpMethod.trim().toUpperCase();
    }

    /**
     * Nome do enum (ex.: "SUCCESS", "HTTP") para comparação por string na query;
     * null quando o filtro não foi informado.
     */
    private String enumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    // ---- Construção da entidade ----

    private AuditLogEntity toEntity(AuditContext ctx) {
        boolean isError = ctx.getStatus() == AuditStatus.ERROR;

        // Todos os campos String são truncados no tamanho EXATO da coluna. Valores
        // derivados de entrada externa (URL → action, X-Forwarded-For → clientIp)
        // podem ser forjados/gigantes; nunca devem estourar o INSERT.
        AuditLogEntity.AuditLogEntityBuilder builder = AuditLogEntity.builder()
                .action(truncate(ctx.getAction(), 100))
                .actionLabel(truncate(ctx.getActionLabel(), 150))
                .source(ctx.getSource())
                .status(ctx.getStatus())
                .message(truncate(ctx.getMessage(), 1000))
                .durationMs(ctx.getDurationMs())
                .httpMethod(truncate(ctx.getHttpMethod(), 10))
                .endpoint(truncate(ctx.getEndpoint(), 500))
                .queryString(truncate(ctx.getQueryString(), 1000))
                .httpStatus(ctx.getHttpStatus())
                .clientIp(truncate(ctx.getClientIp(), 64))
                .userAgent(truncate(ctx.getUserAgent(), 512))
                .origin(truncate(ctx.getOrigin(), 512))
                .traceId(truncate(ctx.getTraceId(), 64))
                .entityType(truncate(ctx.getEntityType(), 100))
                .entityId(ctx.getEntityId());

        applyActor(builder, ctx);

        // Bodies só em erro (controle de volume).
        if (isError) {
            builder.requestBody(truncate(ctx.getRequestBody(), auditProperties.getMaxBodyLength()));
            builder.responseBody(truncate(ctx.getResponseBody(), auditProperties.getMaxBodyLength()));
            builder.requestHeaders(truncate(ctx.getRequestHeaders(), auditProperties.getMaxBodyLength()));
            builder.errorSummary(truncate(resolveErrorSummary(ctx), 2000));
            builder.stackTrace(truncate(buildStackTrace(ctx.getError()), auditProperties.getMaxStackTraceLength()));
        }

        return builder.build();
    }

    /**
     * Define quem é o ator, gravando APENAS a FK do usuário (os dados de
     * nome/e-mail/role são resolvidos por JOIN na leitura) ou, para atores sem
     * conta, um rótulo textual de fallback.
     * <p>
     * Precedência:
     * <ol>
     *   <li>{@code actorUserId} explícito no contexto;</li>
     *   <li>usuário do {@code SecurityContext} (requisições HTTP autenticadas);</li>
     *   <li>fontes não-HTTP (jobs/async/scheduled) → usuário SISTEMA real;</li>
     *   <li>{@code actorLabel} explícito (ex.: e-mail tentado em login que falhou);</li>
     *   <li>"Anônimo/Não autenticado".</li>
     * </ol>
     */
    private void applyActor(AuditLogEntity.AuditLogEntityBuilder builder, AuditContext ctx) {
        if (ctx.getActorUserId() != null) {
            builder.actorUserId(ctx.getActorUserId());
            return;
        }

        UserEntity user = resolveCurrentUser();
        if (user != null) {
            builder.actorUserId(user.getId());
            return;
        }

        // Tarefas automáticas do sistema (jobs/async/scheduled) → usuário SISTEMA real.
        if (ctx.getSource() != null && ctx.getSource() != AuditSource.HTTP) {
            Long systemUserId = resolveSystemUserId();
            if (systemUserId != null) {
                builder.actorUserId(systemUserId);
            } else {
                // Fallback textual caso o usuário SISTEMA não possa ser resolvido.
                builder.actorLabel(ACTOR_SYSTEM_JOB);
            }
            return;
        }

        if (ctx.getActorLabel() != null && !ctx.getActorLabel().isBlank()) {
            builder.actorLabel(truncate(ctx.getActorLabel(), 255));
        } else {
            builder.actorLabel(ACTOR_ANONYMOUS);
        }
    }

    private UserEntity resolveCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserEntity user) {
                return user;
            }
        } catch (Exception ignored) {
            // sem contexto de segurança (ex.: thread de job)
        }
        return null;
    }

    /**
     * Resumo do erro no mesmo espírito do e-mail de report de erro:
     * {@code ClassName: message} acrescido da causa raiz quando diferente.
     */
    private String resolveErrorSummary(AuditContext ctx) {
        if (ctx.getErrorSummary() != null && !ctx.getErrorSummary().isBlank()) {
            return ctx.getErrorSummary();
        }

        Throwable error = ctx.getError();
        if (error == null) {
            return ctx.getMessage();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(error.getClass().getSimpleName());
        if (error.getMessage() != null) {
            sb.append(": ").append(error.getMessage());
        }

        Throwable root = rootCause(error);
        if (root != null && root != error) {
            sb.append(" | Causa raiz: ").append(root.getClass().getSimpleName());
            if (root.getMessage() != null) {
                sb.append(": ").append(root.getMessage());
            }
        }
        return sb.toString();
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current.getCause() != null && current.getCause() != current && depth < 20) {
            current = current.getCause();
            depth++;
        }
        return current;
    }

    private String buildStackTrace(Throwable error) {
        if (error == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
        }
        return sw.toString();
    }

    private static final String TRUNCATION_SUFFIX = "... [truncado]";

    /**
     * Trunca o valor garantindo que o resultado NUNCA exceda {@code max}
     * caracteres (o sufixo de truncamento é contabilizado dentro do limite).
     * Essencial para não estourar o tamanho das colunas no INSERT — uma falha de
     * persistência de auditoria não pode poluir o log nem depender do tamanho da
     * entrada (URL/headers podem ser forjados/gigantes por scanners).
     */
    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        if (max <= TRUNCATION_SUFFIX.length()) {
            return value.substring(0, max);
        }
        return value.substring(0, max - TRUNCATION_SUFFIX.length()) + TRUNCATION_SUFFIX;
    }
}
