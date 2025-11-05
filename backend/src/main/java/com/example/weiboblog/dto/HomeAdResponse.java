package com.example.weiboblog.dto;

import java.time.OffsetDateTime;

public record HomeAdResponse(
        Long id,
        String title,
        String imageUrl,
        String targetUrl,
        int displayOrder,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
