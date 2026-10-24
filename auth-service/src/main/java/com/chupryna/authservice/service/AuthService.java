package com.chupryna.authservice.service;

import com.chupryna.authservice.config.JwtProperties;
import com.chupryna.authservice.dto.AuthResponse;
import com.chupryna.authservice.dto.LoginRequest;
import com.chupryna.authservice.dto.RefreshTokenRequest;
import com.chupryna.authservice.dto.RegisterRequest;
import com.chupryna.authservice.entity.Role;
import com.chupryna.authservice.entity.User;
import com.chupryna.authservice.exception.UserAlreadyExistsException;
import com.chupryna.authservice.exception.UserNotFoundException;
import com.chupryna.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(Role.ROLE_USER)
            .enabled(true)
            .build();

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createSession(user.getId());

        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if(!user.isEnabled()) {
            throw new DisabledException("User with email " + request.email() + " is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createSession(user.getId());

        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtProperties.accessTokenTtl().toSeconds());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        var result = refreshTokenService.rotate(
            request.refreshToken(),
            userId -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
                if (!user.isEnabled()) {
                    throw new DisabledException("User account is disabled");
                }
                return jwtService.generateAccessToken(user);
            }
        );

        return switch (result) {
            case RotationResult.Success s ->
                new AuthResponse(s.accessToken(), s.refreshToken(), "Bearer", jwtProperties.accessTokenTtl().toSeconds());
            case RotationResult.InvalidOrExpired ignored ->
                throw new BadCredentialsException("Invalid or expired refresh token");
            case RotationResult.Compromised c -> {
                log.warn("SECURITY ALERT: Token reuse detected! Revoking familyId={} for userId={}", c.familyId(), c.userId());
                throw new BadCredentialsException("Token reuse detected. Session revoked.");
            }
        };
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }
}
