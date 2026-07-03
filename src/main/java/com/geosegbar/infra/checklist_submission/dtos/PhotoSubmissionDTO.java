package com.geosegbar.infra.checklist_submission.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoSubmissionDTO {

    /**
     * Fluxo LEGADO (base64): imagem embutida no JSON. Usado pelo endpoint
     * {@code POST /checklist-responses/submit}.
     */
    private String base64Image;

    /**
     * Fluxo PRESIGNED (direto-pro-S3): chave S3 (objectKey) devolvida pelo
     * endpoint de presign, apontando para a imagem já enviada. Usado pelo
     * endpoint {@code POST /checklist-responses/submit-presigned}. Nesse fluxo a
     * imagem NÃO trafega no JSON — só a referência.
     */
    private String objectKey;

    private String fileName;

    private String contentType;
}
