package com.example.weiboblog.dto;

import com.example.weiboblog.domain.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank
        @Size(max = 500)
        String content,

        @NotNull
        PostVisibility visibility,

        @Size(max = 20)
        List<Long> allowedUserIds
) {
}
