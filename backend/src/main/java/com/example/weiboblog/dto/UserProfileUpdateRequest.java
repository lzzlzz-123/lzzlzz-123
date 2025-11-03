package com.example.weiboblog.dto;

import com.example.weiboblog.domain.PrivacySetting;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String displayName,

        @Size(max = 280)
        String bio,

        @Size(max = 255)
        String avatarUrl,

        @Size(max = 280)
        String signature,

        @Size(max = 120)
        String location,

        @NotNull
        PrivacySetting privacySetting
) {
}
