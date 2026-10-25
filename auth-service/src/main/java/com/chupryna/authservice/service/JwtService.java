package com.chupryna.authservice.service;

import com.chupryna.authservice.config.JwtProperties;
import com.chupryna.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractAllClaims(String token) {
        var parser = Jwts.parser()
            .verifyWith(secretKey);

        if (jwtProperties.issuer() != null && !jwtProperties.issuer().isBlank()) {
            parser.requireIssuer(jwtProperties.issuer());
        }
        return parser.build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(jwtProperties.accessTokenTtl())))
            .signWith(secretKey, Jwts.SIG.HS256);

        if (jwtProperties.issuer() != null && !jwtProperties.issuer().isBlank()) {
            builder.issuer(jwtProperties.issuer());
        }
        return builder.compact();
    }

    public Long extractUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token, User user) {
        try {
            Claims claims = extractAllClaims(token);
            return user.getId().toString().equals(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
