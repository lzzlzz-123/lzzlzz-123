package com.example.weiboblog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HotspotPostCreateRequest(
        @NotBlank @Size(max = 500) String content,
        @Size(max = 4) List<@Size(max = 255) String> mediaUrls,
        Long topicId,
        @Min(0) long initialHeat
) {
}
