package com.chupryna.analytics.dto;

import java.util.Map;

public record UrlAnalyticsResponse(
        String shortCode,
        long totalClicks,
        Map<String, Long> topCountries,
        Map<String, Long> devices,
        Map<String, Long> topBrowsers,
        Map<String, Long> topOperatingSystems
) {
}
