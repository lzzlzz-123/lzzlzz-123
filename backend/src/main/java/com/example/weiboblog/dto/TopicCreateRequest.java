package com.example.weiboblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 280) String description
) {
}
