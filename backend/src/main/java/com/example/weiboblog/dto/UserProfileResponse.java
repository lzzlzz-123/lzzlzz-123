package com.example.weiboblog.dto;

import java.time.OffsetDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String displayName,
        String email,
        String bio,
        String signature,
        String location,
        String avatarUrl,
        OffsetDateTime createdAt,
        long followerCount,
        long followingCount,
        long postCount,
        boolean followedByCurrentUser
) {
}
