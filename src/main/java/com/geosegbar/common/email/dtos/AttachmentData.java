package com.geosegbar.common.email.dtos;

/**
 * Holds pre-read attachment data so it can be safely passed to an @Async method
 * after the HTTP request is released.
 */
public record AttachmentData(String filename, byte[] content, String contentType) {

}
