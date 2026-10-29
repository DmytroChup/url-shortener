package com.chupryna.analytics.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "services.url-service")
public record UrlServiceProperties(
    String url,
    Duration connectTimeout,
    Duration readTimeout
) {
}
