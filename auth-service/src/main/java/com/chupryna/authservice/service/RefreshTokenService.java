package com.chupryna.authservice.service;

import com.chupryna.authservice.config.JwtProperties;
import com.chupryna.authservice.model.RefreshTokenSession;
import com.chupryna.authservice.util.HashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    static final String REFRESH_TOKEN_PREFIX = "auth:refresh:token:";
    static final String FAMILY_PREFIX = "auth:refresh:family:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final JwtProperties jwtProperties;

    public String createSession(Long userId) {
        var familyId = UUID.randomUUID().toString();
        var rawToken = generateSecureToken();
        var tokenHash = HashUtils.sha256Hex(rawToken);

        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        var session = RefreshTokenSession.active(userId, familyId, expiresAt);

        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + tokenHash,
                sessionJson,
                jwtProperties.refreshTokenTtl()
            );
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize refresh token session", e);
        }

        redisTemplate.opsForSet().add(FAMILY_PREFIX + familyId, tokenHash);
        redisTemplate.expire(FAMILY_PREFIX + familyId, jwtProperties.refreshTokenTtl());

        return rawToken;
    }

    public RotationResult rotate(String rawOldToken, Function<Long, String> accessTokenGenerator) {
        var oldHash = HashUtils.sha256Hex(rawOldToken);
        String sessionJson = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + oldHash);

        if(sessionJson == null) {
            return new RotationResult.InvalidOrExpired();
        }

        RefreshTokenSession session;
        try {
            session = objectMapper.readValue(sessionJson, RefreshTokenSession.class);
        } catch (JacksonException e) {
            return new RotationResult.InvalidOrExpired();
        }

        if (session.used()) {
            Duration duration = Duration.between(session.usedAt(), Instant.now());
            if (duration.toSeconds() <= 10) {
                return new RotationResult.Success(session.cachedAccessToken(), session.cachedRefreshToken());
            } else {
                revokeFamily(session.familyId());
                return new RotationResult.Compromised(session.familyId(), session.userId());
            }
        }

        String newRawToken = generateSecureToken();
        String newTokenHash = HashUtils.sha256Hex(newRawToken);
        String newAccessToken = accessTokenGenerator.apply(session.userId());

        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        var newSession = RefreshTokenSession.active(session.userId(), session.familyId(), expiresAt);

        try {
            redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + newTokenHash,
                objectMapper.writeValueAsString(newSession),
                jwtProperties.refreshTokenTtl()
            );
            redisTemplate.opsForSet().add(FAMILY_PREFIX + session.familyId(), newTokenHash);

            var usedSession = session.markUsed(Instant.now(), newAccessToken, newRawToken);
            redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + oldHash,
                objectMapper.writeValueAsString(usedSession),
                jwtProperties.refreshTokenTtl()
            );
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize session", e);
        }
        return new RotationResult.Success(newAccessToken, newRawToken);
    }

    public void revokeFamily(String familyId) {
        var hashes = redisTemplate.opsForSet().members(FAMILY_PREFIX + familyId);
        if (hashes != null && !hashes.isEmpty()) {
            var keys = hashes.stream().map(h -> REFRESH_TOKEN_PREFIX + h).toList();
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(FAMILY_PREFIX + familyId);
    }

    public void revokeByRawToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        var tokenHash = HashUtils.sha256Hex(rawRefreshToken);
        String sessionJson = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + tokenHash);
        if (sessionJson != null) {
            try {
                RefreshTokenSession session = objectMapper.readValue(sessionJson, RefreshTokenSession.class);
                revokeFamily(session.familyId());
            } catch (JacksonException e) {
                log.warn("Failed to deserialize session for revocation", e);
            }
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
