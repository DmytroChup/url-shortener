package com.chupryna.authservice.service;

public sealed interface RotationResult {
    record Success(String accessToken, String refreshToken) implements RotationResult {}

    record InvalidOrExpired() implements RotationResult {}

    record Compromised(String familyId, Long userId) implements RotationResult {}
}
