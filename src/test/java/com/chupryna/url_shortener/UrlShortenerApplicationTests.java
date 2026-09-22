package com.chupryna.url_shortener;

import com.chupryna.url_shortener.dto.UrlRequest;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UrlShortenerApplicationTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UrlRepository urlRepository;

    @BeforeEach
    void setUp() {
        urlRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Should successfully shorten URL and perform 302 redirect to original destination")
    void shorten_ValidUrl_RedirectsToOriginalDestination() throws Exception {
        UrlRequest request = new UrlRequest("https://google.com");
        MvcResult result = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortCode = result.getResponse().getContentAsString();
        mockMvc.perform(get("/api/v1/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    @DisplayName("Should return 404 Not Found with ProblemDetail when short code does not exist")
    void redirect_ShortCodeNotFound_Returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/notfnd1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Url not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body has empty URL")
    void shorten_EmptyUrl_ReturnsBadRequest() throws Exception {
        UrlRequest invalidRequest = new UrlRequest("");

        mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").value("URL cannot be empty"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit is exceeded")
    void rateLimit_ExceededRequests_ReturnsTooManyRequests() throws Exception {
        String clientIp = "192.168.1." + new Random().nextInt(200, 255);

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/test")
                    .with(request -> {
                        request.setRemoteAddr(clientIp);
                        return request;
                    }))
                    .andExpect(status().isNotFound());
        }

        mockMvc.perform(get("/api/v1/test")
                        .with(request -> {
                            request.setRemoteAddr(clientIp);
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }

    @Test
    @DisplayName("Should shorten URL, redirect, and record click analytics end-to-end")
    void fullFlow_ShortenRedirectAndTrackAnalytics() throws Exception {
        UrlRequest request = new UrlRequest("https://example.com/some/long/path");

        MvcResult shortenResult = mockMvc.perform(post("/api/v1/shorten")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortCode = shortenResult.getResponse().getContentAsString();
        assertThat(shortCode).hasSize(7);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/" + shortCode))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "https://example.com/some/long/path"));
        }

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(150))
                .untilAsserted(() -> mockMvc.perform(get("/api/v1/" + shortCode + "/analytics"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.shortCode").value(shortCode))
                        .andExpect(jsonPath("$.originalUrl").value("https://example.com/some/long/path"))
                        .andExpect(jsonPath("$.totalClicks").value(3)));
    }

    @Test
    @DisplayName("Should return 404 when requesting analytics for unknown short code")
    void analytics_UnknownShortCode_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/zzzzzzz/analytics"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when shortening an invalid URL")
    void shorten_InvalidUrl_Returns400() throws Exception {
        UrlRequest request = new UrlRequest("javascript:alert(1)");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "toolongcode", "a!_bc_?", "     "})
    @DisplayName("Should return 404 Not Found when short code format does not match 7-char Base62 regex")
    void redirect_InvalidShortCodeFormat_FastFail(String shortCode) throws Exception {
        mockMvc.perform(get("/api/v1/" + shortCode))
                .andExpect(status().isNotFound());
    }
}
