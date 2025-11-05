package com.example.weiboblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HomeAdRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 255) String imageUrl,
        @NotBlank @Size(max = 255) String targetUrl,
        @PositiveOrZero Integer displayOrder,
        Boolean active
) {
}
