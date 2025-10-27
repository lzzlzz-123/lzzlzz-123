package com.example.weiboblog.dto;

public record AuthResponse(String token, UserSummaryDto user) {
}
