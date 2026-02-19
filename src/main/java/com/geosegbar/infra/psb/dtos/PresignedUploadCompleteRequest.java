package com.geosegbar.infra.psb.dtos;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para confirmar que o upload direto ao S3 foi concluído. Laravel chama
 * este endpoint após enviar todos os bytes ao S3.
 */
@Data
public class PresignedUploadCompleteRequest {

    @NotBlank(message = "O uploadId é obrigatório")
    private String uploadId;

    /**
     * ETags retornados pelo S3 para cada parte (apenas multipart). Cada PUT n
     * S3 retorna um ETag no header da resposta.Necessário para o
     * CompleteMultipartUpload do S3.
     */
    private List<CompletedPartDTO> completedParts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompletedPartDTO {

        private int partNumber;
        private String eTag;
    }
}
