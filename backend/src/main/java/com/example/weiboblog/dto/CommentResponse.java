package com.example.weiboblog.dto;

import java.time.OffsetDateTime;

public record CommentResponse(
        Long id,
        String content,
        OffsetDateTime createdAt,
        UserSummaryDto author
) {
}
