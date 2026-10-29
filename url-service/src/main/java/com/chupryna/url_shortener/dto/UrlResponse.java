package com.chupryna.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response payload representing shortened URL details")
public record UrlResponse(
    @Schema(description = "Unique database identifier", example = "1")
    Long id,

    @Schema(description = "Original destination URL", example = "https://github.com/DmytroChup")
    String originalUrl,

    @Schema(description = "Generated 7-character short code", example = "aB7xK9q")
    String shortCode,

    @Schema(description = "Timestamp when the short URL was created", example = "2026-10-27T10:00:00Z")
    Instant createdAt,

    @Schema(description = "Timestamp when the short URL expires", example = "2026-11-26T10:00:00Z")
    Instant expiresAt
) {}
