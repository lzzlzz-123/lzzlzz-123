package com.example.weiboblog.dto;

import com.example.weiboblog.domain.PrivacySetting;

public record UserSummaryDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        boolean admin,
        PrivacySetting privacySetting
) {
}
