package com.chupryna.gateway.util;

import com.chupryna.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtValidator {

    private final SecretKey secretKey;
    private static final String EXPECTED_ISSUER = "url-shortener-auth";

    public JwtValidator(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateAndExtractClaims(String token) throws JwtException {
        return Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(EXPECTED_ISSUER)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
