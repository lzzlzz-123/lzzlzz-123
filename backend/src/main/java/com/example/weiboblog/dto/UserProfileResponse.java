package com.example.weiboblog.dto;

import com.example.weiboblog.domain.PrivacySetting;

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
        PrivacySetting privacySetting,
        OffsetDateTime createdAt,
        long followerCount,
        long followingCount,
        long postCount,
        boolean followedByCurrentUser
) {
}
