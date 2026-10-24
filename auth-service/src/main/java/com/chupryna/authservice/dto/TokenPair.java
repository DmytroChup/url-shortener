package com.chupryna.authservice.dto;

public record TokenPair(
    String accessToken,
    String refreshToken
) {
}
