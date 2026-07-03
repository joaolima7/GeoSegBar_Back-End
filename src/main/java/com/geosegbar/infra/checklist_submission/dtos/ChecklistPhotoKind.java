package com.geosegbar.infra.checklist_submission.dtos;

/**
 * Contexto de uma foto no fluxo presigned — determina o prefixo S3 onde a
 * imagem é armazenada (mantendo a mesma organização do fluxo base64):
 * <ul>
 *   <li>{@code ANSWER}  → prefixo {@code answer-photos/} (foto de resposta);</li>
 *   <li>{@code ANOMALY} → prefixo {@code anomalies/} (foto de anomalia/"outros").</li>
 * </ul>
 */
public enum ChecklistPhotoKind {
    ANSWER,
    ANOMALY
}
