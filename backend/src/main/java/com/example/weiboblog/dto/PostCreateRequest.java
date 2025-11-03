package com.example.weiboblog.dto;

import com.example.weiboblog.domain.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank @Size(max = 500) String content,
        @Size(max = 4) List<@Size(max = 255) String> mediaUrls,
        Long topicId,
        PostVisibility visibility,
        @Size(max = 20) List<Long> allowedUserIds
) {
    public PostCreateRequest(String content, List<String> mediaUrls, Long topicId) {
        this(content, mediaUrls, topicId, null, null);
    }
}
