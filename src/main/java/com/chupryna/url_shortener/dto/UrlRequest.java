package com.chupryna.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
        String url
) {
}
