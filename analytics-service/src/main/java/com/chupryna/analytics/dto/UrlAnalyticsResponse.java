package com.chupryna.analytics.dto;

public record UrlAnalyticsResponse(
        String shortCode,
        long totalClicks
) {
}
