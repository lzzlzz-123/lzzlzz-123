package com.example.weiboblog.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String content,
        List<String> mediaUrls,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UserSummaryDto author,
        long likeCount,
        long commentCount,
        boolean likedByCurrentUser
) {
}
