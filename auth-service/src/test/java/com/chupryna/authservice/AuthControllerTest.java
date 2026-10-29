package com.chupryna.authservice;

import com.chupryna.authservice.dto.AuthResponse;
import com.chupryna.authservice.dto.LoginRequest;
import com.chupryna.authservice.dto.RefreshTokenRequest;
import com.chupryna.authservice.dto.RegisterRequest;
import com.chupryna.authservice.model.RefreshTokenSession;
import com.chupryna.authservice.repository.UserRepository;
import com.chupryna.authservice.util.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        var connection = redisTemplate.getConnectionFactory().getConnection();
        connection.serverCommands().flushAll();
    }

    @Test
    @DisplayName("Should successfully register a new user and return tokens")
    void register_ValidRequest_ReturnsCreatedAndTokens() throws Exception {
        var registerRequest = new RegisterRequest("newuser@example.com", "securePassword123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));

        var userInDb = userRepository.findByEmail("newuser@example.com");
        assertThat(userInDb).isPresent();
        assertThat(userInDb.get().getPasswordHash()).isNotEqualTo("securePassword123");
    }

    @Test
    @DisplayName("Should reject registration when email is already taken with 409 Conflict")
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        var registerRequest = new RegisterRequest("duplicate@example.com", "securePassword123");

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Second registration with same email returns 409 Conflict
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User with email duplicate@example.com already exists"));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_ValidCredentials_ReturnsOkAndTokens() throws Exception {
        var registerRequest = new RegisterRequest("loginuser@example.com", "correctPassword123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var loginRequest = new LoginRequest("loginuser@example.com", "correctPassword123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject login with invalid password with 401 Unauthorized")
    void login_InvalidPassword_ReturnsUnauthorized() throws Exception {
        var registerRequest = new RegisterRequest("loginuser@example.com", "correctPassword123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        var badLoginRequest = new LoginRequest("loginuser@example.com", "wrongPassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLoginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should successfully rotate refresh token and issue new token pair")
    void refresh_ValidToken_ReturnsNewTokens() throws Exception {
        var registerRequest = new RegisterRequest("rotateuser@example.com", "securePassword123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse initialAuth = objectMapper.readValue(
                regResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // Perform rotation with initial refresh token
        var refreshRequest = new RefreshTokenRequest(initialAuth.refreshToken());
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse rotatedAuth = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // Verified that a new distinct refresh token was issued
        assertThat(rotatedAuth.refreshToken()).isNotEqualTo(initialAuth.refreshToken());
    }

    @Test
    @DisplayName("Should detect token reuse past grace period and revoke entire family")
    void refresh_TokenReusePastGracePeriod_RevokesFamily() throws Exception {
        // 1. Register user
        var registerRequest = new RegisterRequest("reuse@example.com", "securePassword123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse initialAuth = objectMapper.readValue(
                regResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // 2. Rotate token first time (normal rotation)
        var refreshRequest1 = new RefreshTokenRequest(initialAuth.refreshToken());
        MvcResult rotateResult1 = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest1)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse secondAuth = objectMapper.readValue(
                rotateResult1.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // 3. Simulate passage of time beyond the 10-second grace period for the old token
        String oldTokenHash = HashUtils.sha256Hex(initialAuth.refreshToken());
        String oldSessionKey = "auth:refresh:token:" + oldTokenHash;
        String oldSessionJson = redisTemplate.opsForValue().get(oldSessionKey);
        assertThat(oldSessionJson).isNotNull();

        RefreshTokenSession oldSession = objectMapper.readValue(oldSessionJson, RefreshTokenSession.class);
        // Artificially move usedAt timestamp 30 seconds into the past
        RefreshTokenSession expiredGraceSession = new RefreshTokenSession(
                oldSession.userId(),
                oldSession.familyId(),
                true,
                Instant.now().minusSeconds(30),
                oldSession.expiresAt(),
                oldSession.cachedAccessToken(),
                oldSession.cachedRefreshToken()
        );
        redisTemplate.opsForValue().set(oldSessionKey, objectMapper.writeValueAsString(expiredGraceSession));

        // 4. Attacker attempts to reuse the old refresh token -> triggers Reuse Detection
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest1)))
                .andExpect(status().isUnauthorized());

        // 5. Verify the entire token family was revoked: legitimate second token is also invalidated!
        var refreshRequest2 = new RefreshTokenRequest(secondAuth.refreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should successfully logout and invalidate refresh token")
    void logout_ValidToken_InvalidatesSession() throws Exception {
        var registerRequest = new RegisterRequest("logout@example.com", "securePassword123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                regResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // Logout
        var logoutRequest = new RefreshTokenRequest(auth.refreshToken());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // Attempting to refresh after logout returns 401 Unauthorized
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }
}
