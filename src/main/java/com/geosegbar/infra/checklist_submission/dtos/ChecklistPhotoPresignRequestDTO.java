package com.geosegbar.infra.checklist_submission.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FASE 1 (presign) — requisição em lote para obter URLs pré-assinadas de upload
 * direto ao S3, uma por imagem do checklist. Nenhum binário trafega aqui: só os
 * metadados de cada foto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistPhotoPresignRequestDTO {

    @NotEmpty(message = "A lista de imagens (items) não pode ser vazia.")
    @Valid
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        /**
         * Referência gerada pelo front para casar esta imagem com a resposta do
         * presign (e depois usar o objectKey na submissão). Ex.: "p1", um UUID.
         */
        @NotBlank(message = "clientRef é obrigatório para cada imagem.")
        private String clientRef;

        /**
         * ANSWER (padrão) ou ANOMALY — define o prefixo S3. Se nulo, assume ANSWER.
         */
        private ChecklistPhotoKind kind;

        @NotBlank(message = "fileName é obrigatório para cada imagem.")
        private String fileName;

        /**
         * MIME type (ex.: image/jpeg, image/png). Recomendado: se informado, é
         * assinado na URL e o front DEVE enviar o mesmo header Content-Type no PUT.
         */
        private String contentType;

        /**
         * Tamanho em bytes (opcional, informativo).
         */
        private Long sizeBytes;
    }
}
