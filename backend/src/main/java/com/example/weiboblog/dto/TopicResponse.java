package com.example.weiboblog.dto;

import java.time.OffsetDateTime;

public record TopicResponse(
        Long id,
        String name,
        String description,
        long heat,
        long memberCount,
        boolean joined,
        UserSummaryDto owner,
        OffsetDateTime createdAt
) {
}
