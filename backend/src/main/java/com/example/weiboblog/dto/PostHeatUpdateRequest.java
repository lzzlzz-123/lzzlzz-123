package com.example.weiboblog.dto;

import jakarta.validation.constraints.Min;

public record PostHeatUpdateRequest(
        @Min(0) long heat
) {
}
