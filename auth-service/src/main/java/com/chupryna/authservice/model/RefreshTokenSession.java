package com.chupryna.authservice.model;

import java.time.Instant;

public record RefreshTokenSession(
    Long userId,
    String familyId,
    boolean used,
    Instant usedAt,
    Instant expiresAt,
    String cachedAccessToken,
    String cachedRefreshToken
) {
    public static RefreshTokenSession active(Long userId, String familyId, Instant expiresAt) {
        return new RefreshTokenSession(
            userId, familyId, false, null, expiresAt, null, null);
    }

    public RefreshTokenSession markUsed(Instant usedAt, String newAccessToken, String newRefreshToken) {
        return new RefreshTokenSession(
            this.userId,
            this.familyId,
            true,
            usedAt,
            this.expiresAt,
            newAccessToken,
            newRefreshToken
        );
    }
}
