package com.example.weiboblog.dto;

import jakarta.validation.constraints.Min;

public record TopicHeatUpdateRequest(
        @Min(0) long heat
) {
}
