package com.chupryna.url_shortener.config.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.url")
@Validated
public record UrlProperties(
        @NotNull Duration cacheTtl,
        @NotNull Duration negativeCacheTtl,
        @NotNull Duration expiredCacheTtl,
        @Positive int defaultTtlDays,
        @Positive int maxSaveRetries
) {
}
