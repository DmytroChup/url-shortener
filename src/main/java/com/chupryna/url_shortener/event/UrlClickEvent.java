package com.chupryna.url_shortener.event;

import java.time.Instant;

public record UrlClickEvent(
        String shortCode,
        String userAgent,
        String maskedIpAddress,
        Instant clickedAt,
        String referer
) {
}
