package com.geosegbar.infra.audit.dtos;

import java.time.LocalDateTime;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.infra.audit.persistence.projections.AuditLogSummaryProjection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Visão de auditoria para o usuário comum: apenas campos básicos (quem, quando,
 * o que, resultado, mensagem). Os dados do ator ({@code actorName},
 * {@code actorEmail}) vêm do JOIN com a tabela de usuários (sempre atualizados);
 * para atores sem conta (job/anônimo/login com falha), usa-se {@code actorLabel}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogSummaryDTO {

    private Long id;
    private LocalDateTime occurredAt;
    private Long actorUserId;
    private String actorName;
    private String actorEmail;
    private String action;
    private String actionLabel;
    private AuditSource source;
    private AuditStatus status;
    private String message;

    public static AuditLogSummaryDTO fromProjection(AuditLogSummaryProjection p) {
        return AuditLogSummaryDTO.builder()
                .id(p.getId())
                .occurredAt(p.getOccurredAt())
                .actorUserId(p.getActorUserId())
                .actorName(AuditActorResolver.resolveName(p.getUserName(), p.getActorLabel(), p.getActorUserId()))
                .actorEmail(p.getUserEmail())
                .action(p.getAction())
                .actionLabel(p.getActionLabel())
                .source(p.getSource())
                .status(p.getStatus())
                .message(p.getMessage())
                .build();
    }
}
