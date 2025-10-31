package com.example.weiboblog.dto;

public record TopicSummaryDto(
        Long id,
        String name,
        String description,
        long heat,
        long memberCount,
        boolean joined
) {
}
