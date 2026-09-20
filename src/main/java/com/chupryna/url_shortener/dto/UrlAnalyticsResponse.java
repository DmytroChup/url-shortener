package com.chupryna.url_shortener.dto;

public record UrlAnalyticsResponse(
        String shortCode,
        String originalUrl,
        long totalClicks
) {
}
