package com.example.weiboblog.dto;

import java.util.List;

public record UserConnectionsResponse(
        List<UserSummaryDto> followers,
        List<UserSummaryDto> followees
) {
}
