package com.chupryna.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for URL shortening")
public record UrlRequest(
        @Schema(
                description = "Original long URL to shorten",
                example = "https://github.com/DmytroChup",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "URL cannot be empty")
        String url
) {
}
