package com.geosegbar.infra.psb.dtos;

/**
 * URL pré-assinada para baixar um arquivo direto do S3.
 *
 * Alternativa ao redirect 302, para o front que preferir receber a URL e
 * navegar por conta própria — window.location ou âncora não passam por CORS,
 * enquanto um XHR que segue redirect para outro domínio passa.
 */
public record PresignedDownloadResponse(
        String url,
        String filename,
        String contentType,
        Long size) {

}
