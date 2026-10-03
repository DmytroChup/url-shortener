package com.chupryna.url_shortener.event;

import java.time.Instant;
import java.util.UUID;

public record UrlClickEvent(
        UUID eventId,
        String shortCode,
        String userAgent,
        String maskedIpAddress,
        Instant clickedAt,
        String referer
) {
}
