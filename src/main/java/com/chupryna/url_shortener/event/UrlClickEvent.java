package com.chupryna.url_shortener.event;

import java.time.LocalDateTime;

public record UrlClickEvent(
        String shortCode,
        String userAgent,
        String maskedIpAddress,
        LocalDateTime clickedAt,
        String referer
) {
}
