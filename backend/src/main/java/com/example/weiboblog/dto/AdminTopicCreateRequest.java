package com.example.weiboblog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTopicCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 280) String description,
        @Min(0) long initialHeat
) {
}
