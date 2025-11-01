package com.example.weiboblog.dto;

public record MediaUploadResponse(
        String url,
        String contentType,
        String mediaType,
        long size,
        String originalFilename
) {
}
