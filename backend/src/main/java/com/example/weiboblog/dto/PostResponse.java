package com.example.weiboblog.dto;

import com.example.weiboblog.domain.PostVisibility;

import java.time.OffsetDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String content,
        List<String> mediaUrls,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UserSummaryDto author,
        TopicLightDto topic,
        long likeCount,
        long commentCount,
        long heat,
        boolean inHotspot,
        boolean likedByCurrentUser,
        boolean ownedByCurrentUser,
        PostVisibility visibility,
        List<Long> allowedUserIds
) {
}
