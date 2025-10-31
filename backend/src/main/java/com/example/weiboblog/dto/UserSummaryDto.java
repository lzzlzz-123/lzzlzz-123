package com.example.weiboblog.dto;

public record UserSummaryDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        boolean admin
) {
}
