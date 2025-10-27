package com.example.weiboblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank @Size(max = 280) String content
) {
}
