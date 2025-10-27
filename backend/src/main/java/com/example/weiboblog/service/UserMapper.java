package com.example.weiboblog.service;

import com.example.weiboblog.domain.User;
import com.example.weiboblog.dto.UserProfileResponse;
import com.example.weiboblog.dto.UserSummaryDto;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserSummaryDto toSummary(User user) {
        return new UserSummaryDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }

    public static UserProfileResponse toProfile(User user,
                                                long followerCount,
                                                long followingCount,
                                                long postCount,
                                                boolean followedByCurrentUser) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                followerCount,
                followingCount,
                postCount,
                followedByCurrentUser
        );
    }
}
