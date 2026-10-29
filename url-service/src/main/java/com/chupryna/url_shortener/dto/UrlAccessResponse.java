package com.chupryna.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload indicating if a user has access to a URL")
public record UrlAccessResponse(
    @Schema(description = "True if access is granted, false otherwise", example = "true")
    boolean allowed
) {
}
