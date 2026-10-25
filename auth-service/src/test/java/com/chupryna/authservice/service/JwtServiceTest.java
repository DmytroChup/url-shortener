package com.chupryna.authservice.service;

import com.chupryna.authservice.config.JwtProperties;
import com.chupryna.authservice.entity.Role;
import com.chupryna.authservice.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private static final String SECRET = "58fd7fc9a69d5a74d1b12c6bdc477b12a858f4392c2149f1b28f33346dd7aaa0";
    private static final String ISSUER = "url-shortener-auth";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                SECRET,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                ISSUER
        );
        jwtService = new JwtService(properties);

        testUser = User.builder()
                .id(42L)
                .email("developer@example.com")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should generate valid access token with correct claims")
    void generateAccessToken_ValidUser_GeneratesCorrectClaims() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
        assertThat(jwtService.extractUserIdFromToken(token)).isEqualTo(42L);

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email")).isEqualTo("developer@example.com");
        assertThat(claims.get("role")).isEqualTo("ROLE_USER");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    @DisplayName("Should reject token when validated against another user")
    void isTokenValid_DifferentUser_ReturnsFalse() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        User anotherUser = User.builder()
                .id(999L)
                .email("intruder@example.com")
                .role(Role.ROLE_USER)
                .build();

        // Then
        assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    @DisplayName("Should return false for tampered token")
    void isTokenValid_TamperedToken_ReturnsFalse() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        String tamperedToken = token + "corrupted";

        // Then
        assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        // Given: JwtService configured with negative expiration
        JwtProperties expiredProperties = new JwtProperties(
                SECRET,
                Duration.ofSeconds(-10),
                Duration.ofDays(7),
                ISSUER
        );
        JwtService expiredJwtService = new JwtService(expiredProperties);

        // When
        String expiredToken = expiredJwtService.generateAccessToken(testUser);

        // Then
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }
}
