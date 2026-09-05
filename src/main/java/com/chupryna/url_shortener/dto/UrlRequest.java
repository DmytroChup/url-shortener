package com.chupryna.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;

public record UrlRequest(
        @NotBlank(message = "URL cannot be empty") String url
) {
}
