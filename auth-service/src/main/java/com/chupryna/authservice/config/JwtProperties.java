package com.chupryna.authservice.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

    @NotBlank
    @Size(min = 32, message = "JWT secret must be at least 32 characters long (256 bits for HMAC-SHA256)")
    String secret,

    @NotNull
    Duration accessTokenTtl,

    @NotNull
    Duration refreshTokenTtl,

    String issuer
) {
}
