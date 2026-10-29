package com.chupryna.analytics.controller;

import com.chupryna.analytics.BaseIntegrationTest;
import com.chupryna.url_shortener.security.SecurityHeaders;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Url Analytics Authorization Integration Tests")
class UrlAnalyticsAuthorizationIntegrationTest extends BaseIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureWireMockUrl(DynamicPropertyRegistry registry) {
        registry.add("services.url-service.url", () -> wireMockServer.baseUrl());
    }

    @BeforeEach
    void resetState() {
        wireMockServer.resetAll();
        var cache = cacheManager.getCache("url-access");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("Should return 403 Forbidden when X-User-Id header is missing")
    void getUrlAnalytics_MissingUserId_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/{shortCode}", "code101"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Access denied or resource not found"));

        wireMockServer.verify(0, getRequestedFor(anyUrl()));
    }

    @Test
    @DisplayName("Should return 200 OK and cache result when user owns the URL")
    void getUrlAnalytics_OwnerUser_ReturnsOkAndCachesResponse() throws Exception {
        String shortCode = "own1001";
        Long userId = 52L;

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/internal/api/v1/urls/" + shortCode + "/access"))
            .withQueryParam("userId", equalTo(String.valueOf(userId)))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"allowed\": true}")));

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                .header(SecurityHeaders.X_USER_ID, userId)
                .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode));

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                .header(SecurityHeaders.X_USER_ID, userId)
                .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_USER))
            .andExpect(status().isOk());

        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/internal/api/v1/urls/" + shortCode + "/access"))
            .withQueryParam("userId", equalTo(String.valueOf(userId))));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user is not the owner")
    void getUrlAnalytics_StrangerUser_ReturnsForbidden() throws Exception {
        String shortCode = "own1002";
        Long strangerId = 99L;

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/internal/api/v1/urls/" + shortCode + "/access"))
            .withQueryParam("userId", equalTo(String.valueOf(strangerId)))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"allowed\": false}")));

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                .header(SecurityHeaders.X_USER_ID, strangerId)
                .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_USER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Access denied or resource not found"));
    }

    @Test
    @DisplayName("Should return 200 OK without calling url-service when user has ROLE_ADMIN")
    void getUrlAnalytics_AdminRole_BypassesUrlServiceAndReturnsOk() throws Exception {
        String shortCode = "admin01";

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                .header(SecurityHeaders.X_USER_ID, 999L)
                .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_ADMIN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").value(shortCode));

        wireMockServer.verify(0, getRequestedFor(anyUrl()));
    }

    @Test
    @DisplayName("Should fail-closed and return 403 Forbidden when url-service returns 500 error")
    void getUrlAnalytics_UrlServiceError_FailsClosedWithForbidden() throws Exception {
        String shortCode = "err1001";
        Long userId = 52L;

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/internal/api/v1/urls/" + shortCode + "/access"))
            .willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                .header(SecurityHeaders.X_USER_ID, userId)
                .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_USER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Access denied or resource not found"));
    }

    @Test
    @DisplayName("Should fail-closed and return 403 Forbidden when url-service times out")
    void getUrlAnalytics_UrlServiceTimeout_FailsClosedWithForbidden() throws Exception {
        String shortCode = "timeout";
        Long userId = 42L;

        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/internal/api/v1/urls/" + shortCode + "/access"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"allowed\": true}")
                .withFixedDelay(2500)));

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                .header(SecurityHeaders.X_USER_ID, userId)
                .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_USER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Access denied or resource not found"));
    }
}
