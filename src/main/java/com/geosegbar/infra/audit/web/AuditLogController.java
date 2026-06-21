package com.geosegbar.infra.audit.web;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.infra.audit.dtos.AuditLogDetailDTO;
import com.geosegbar.infra.audit.dtos.AuditLogFilterDTO;
import com.geosegbar.infra.audit.dtos.AuditLogSummaryDTO;
import com.geosegbar.infra.audit.services.AuditService;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints de leitura da auditoria.
 * <ul>
 *   <li>{@code GET /audit-logs}: visão do usuário comum (campos básicos).</li>
 *   <li>{@code GET /audit-logs/admin} e {@code /audit-logs/{id}/details}: visão
 *       admin/dev (todos os campos, incl. request/response/erro/stacktrace).</li>
 * </ul>
 * Toda a rota exige autenticação (cai em {@code anyRequest().authenticated()}); a
 * visão admin é restrita por papel.
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<Page<AuditLogSummaryDTO>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) AuditSource source,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        AuditLogFilterDTO filter = buildFilter(startDate, endDate, actorUserId, actorEmail,
                action, status, source, httpMethod, entityType, entityId);
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<AuditLogSummaryDTO> result = auditService.findSummaries(filter, pageable);
        return ResponseEntity.ok(
                WebResponseEntity.success(result, "Auditoria obtida com sucesso!"));
    }

    @GetMapping("/admin")
    public ResponseEntity<WebResponseEntity<Page<AuditLogDetailDTO>>> listAdmin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) AuditSource source,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        AuthenticatedUserUtil.checkAdminPermission();

        AuditLogFilterDTO filter = buildFilter(startDate, endDate, actorUserId, actorEmail,
                action, status, source, httpMethod, entityType, entityId);
        Pageable pageable = buildPageable(page, size, sortBy, sortDirection);

        Page<AuditLogDetailDTO> result = auditService.findDetails(filter, pageable);
        return ResponseEntity.ok(
                WebResponseEntity.success(result, "Auditoria detalhada obtida com sucesso!"));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<WebResponseEntity<AuditLogDetailDTO>> details(@PathVariable Long id) {
        AuthenticatedUserUtil.checkAdminPermission();
        AuditLogDetailDTO result = auditService.findDetailById(id);
        return ResponseEntity.ok(
                WebResponseEntity.success(result, "Detalhe de auditoria obtido com sucesso!"));
    }

    private AuditLogFilterDTO buildFilter(LocalDateTime startDate, LocalDateTime endDate,
            Long actorUserId, String actorEmail, String action, AuditStatus status,
            AuditSource source, String httpMethod, String entityType, Long entityId) {
        AuditLogFilterDTO filter = new AuditLogFilterDTO();
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setActorUserId(actorUserId);
        filter.setActorEmail(actorEmail);
        filter.setAction(action);
        filter.setStatus(status);
        filter.setSource(source);
        filter.setHttpMethod(httpMethod);
        filter.setEntityType(entityType);
        filter.setEntityId(entityId);
        return filter;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
