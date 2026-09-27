package com.chupryna.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for URL shortening")
public record UrlRequest(
        @Schema(
                description = "Original long URL to shorten",
                example = "https://github.com/DmytroChup",
                maxLength = 2048,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "URL cannot be empty")
        @Size(max = 2048, message = "URL is too long")
        String url,

        // null - default
        @Schema(
                description = "Optional number of days until the short link expires. " +
                        "If omitted, the link defaults to a 30-day expiration.",
                example = "30",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Positive(message = "TTL must be a positive number of days")
        @Max(value = 365, message = "TTL cannot exceed 365 days")
        Integer ttlDays
) {
        public UrlRequest(String url) {
                this(url, null);
        }
}
