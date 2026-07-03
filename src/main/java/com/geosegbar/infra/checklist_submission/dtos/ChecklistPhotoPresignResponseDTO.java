package com.geosegbar.infra.checklist_submission.dtos;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FASE 1 (presign) — resposta com uma URL pré-assinada por imagem. O front faz
 * {@code PUT uploadUrl} com o binário e guarda o {@code objectKey} para referenciar
 * a imagem na submissão (FASE 3).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistPhotoPresignResponseDTO {

    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {

        /**
         * Mesma referência enviada na requisição (para o front casar item↔resposta).
         */
        private String clientRef;

        /**
         * Chave S3 do objeto. É este valor que vai em {@code photos[].objectKey}
         * na submissão.
         */
        private String objectKey;

        /**
         * URL pré-assinada para o {@code PUT} direto ao S3 (expira em ~15min).
         */
        private String uploadUrl;

        /**
         * Instante de expiração da URL assinada.
         */
        private Instant expiresAt;
    }
}
