package com.geosegbar.infra.psb.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta contendo URLs pré-assinadas para upload multipart direto ao S3.
 * TODOS os uploads usam multipart — mesmo arquivos pequenos. Partes de 5MB
 * (mínimo S3) a 50MB conforme tamanho do arquivo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadInitResponse {

    /**
     * Identificador único do upload (UUID) — usado para confirmar/cancelar
     */
    private String uploadId;

    /**
     * Chave S3 onde o arquivo será armazenado
     */
    private String s3Key;

    /**
     * @deprecated Não utilizado — todos os uploads são multipart.
     */
    private String uploadUrl;

    /**
     * ID do multipart upload do S3 (apenas para uploads multipart). Necessário
     * para confirmar/abortar o upload multipart.
     */
    private String s3UploadId;

    /**
     * Tamanho recomendado de cada parte em bytes (apenas multipart)
     */
    private Long partSize;

    /**
     * Total de partes esperadas (apenas multipart)
     */
    private Integer totalParts;

    /**
     * URLs pré-assinadas para cada parte (apenas multipart)
     */
    private List<PresignedPartUrl> parts;

    /**
     * Se true, o upload deve ser feito via multipart
     */
    private boolean multipart;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresignedPartUrl {

        private int partNumber;
        private String uploadUrl;
    }
}
