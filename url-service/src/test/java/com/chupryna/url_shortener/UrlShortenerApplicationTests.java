package com.chupryna.url_shortener;

import com.chupryna.url_shortener.dto.UrlRequest;
import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.entity.outbox.OutboxStatus;
import com.chupryna.url_shortener.repository.OutboxEventRepository;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.security.SecurityHeaders;
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
import java.time.Instant;

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

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        urlRepository.deleteAll();
        outboxEventRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Should successfully shorten URL and perform 302 redirect to original destination")
    void shorten_ValidUrl_RedirectsToOriginalDestination() throws Exception {
        UrlRequest request = new UrlRequest("https://google.com");
        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortCode = result.getResponse().getContentAsString();
        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    @DisplayName("Should return 404 Not Found with ProblemDetail when short code does not exist")
    void redirect_ShortCodeNotFound_Returns404WithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/urls/notfnd1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Url not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body has empty URL")
    void shorten_EmptyUrl_ReturnsBadRequest() throws Exception {
        UrlRequest invalidRequest = new UrlRequest("");

        mockMvc.perform(post("/api/v1/urls/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").value("URL cannot be empty"));
    }

    @Test
    @DisplayName("Should shorten URL, redirect, and record click analytics end-to-end")
    void fullFlow_ShortenAndRedirect() throws Exception {
        UrlRequest request = new UrlRequest("https://example.com/some/long/path");

        MvcResult shortenResult = mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String shortCode = shortenResult.getResponse().getContentAsString();
        assertThat(shortCode).hasSize(7);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/urls/" + shortCode))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "https://example.com/some/long/path"));
        }

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var events = outboxEventRepository.findAll();
                    assertThat(events).hasSize(3);
                    assertThat(events).allMatch(e -> e.getStatus() == OutboxStatus.SENT && e.getSentAt() != null);
                });
    }

    @Test
    @DisplayName("Should return 400 when shortening an invalid URL")
    void shorten_InvalidUrl_Returns400() throws Exception {
        UrlRequest request = new UrlRequest("javascript:alert(1)");

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "toolongcode", "a!_bc_?", "     "})
    @DisplayName("Should return 404 Not Found when short code format does not match 7-char Base62 regex")
    void redirect_InvalidShortCodeFormat_FastFail(String shortCode) throws Exception {
        mockMvc.perform(get("/api/v1/urls/" + shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 410 Gone once TTL naturally expires in both DB and Redis")
    void redirect_ShortTtlLink_ExpiresNaturallyInCacheAndDatabase() throws Exception {
        Url url = new Url();
        url.setShortCode("zTest01");
        url.setOriginalUrl("https://example.com/short-ttl-test");
        url.setExpiresAt(Instant.now().plus(Duration.ofSeconds(1)));
        urlRepository.save(url);

        mockMvc.perform(get("/api/v1/urls/" + url.getShortCode()))
                .andExpect(status().isFound());

        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() ->
                        mockMvc.perform(get("/api/v1/urls/" + url.getShortCode()))
                                .andExpect(status().isGone())
                );
    }

    @Test
    @DisplayName("Should persist userId in database when X-User-Id header is present on shorten")
    void shorten_WithUserIdHeader_PersistsUserIdInDatabase() throws Exception {
        UrlRequest request = new UrlRequest("https://example.com/user-link");

        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                .header(SecurityHeaders.X_USER_ID, 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String shortCode = result.getResponse().getContentAsString();
        Url savedUrl = urlRepository.findByShortCode(shortCode).orElseThrow();

        assertThat(savedUrl.getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Should persist null userId in database when X-User-Id header is missing (guest flow)")
    void shorten_WithoutUserIdHeader_PersistsNullUserId() throws Exception {
        UrlRequest request = new UrlRequest("https://example.com/guest-link");

        MvcResult result = mockMvc.perform(post("/api/v1/urls/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String shortCode = result.getResponse().getContentAsString();
        Url savedUrl = urlRepository.findByShortCode(shortCode).orElseThrow();

        assertThat(savedUrl.getUserId()).isNull();
    }

    @Test
    @DisplayName("Should return only URLs belonging to the authenticated user ordered by createdAt desc")
    void getMyUrls_WithUserIdHeader_ReturnsUserUrlsOrdered() throws Exception {
        Url userUrl1 = new Url();
        userUrl1.setShortCode("user101");
        userUrl1.setOriginalUrl("https://example.com/first");
        userUrl1.setUserId(100L);
        userUrl1.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        urlRepository.save(userUrl1);

        Url userUrl2 = new Url();
        userUrl2.setShortCode("user102");
        userUrl2.setOriginalUrl("https://example.com/second");
        userUrl2.setUserId(100L);
        userUrl2.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        urlRepository.save(userUrl2);

        Url otherUserUrl = new Url();
        otherUserUrl.setShortCode("other01");
        otherUserUrl.setOriginalUrl("https://example.com/other");
        otherUserUrl.setUserId(200L);
        otherUserUrl.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        urlRepository.save(otherUserUrl);

        mockMvc.perform(get("/api/v1/urls/me")
                .header(SecurityHeaders.X_USER_ID, 100L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].shortCode").value("user102"))
            .andExpect(jsonPath("$[1].shortCode").value("user101"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when X-User-Id header is missing on /me endpoint")
    void getMyUrls_WithoutUserIdHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/urls/me"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 200 OK with allowed=true when user owns the URL")
    void checkAccess_OwnerUser_ReturnsOkWithAllowedTrue() throws Exception {
        Url url = new Url();
        url.setShortCode("own1001");
        url.setOriginalUrl("https://example.com/owned");
        url.setUserId(100L);
        url.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        urlRepository.save(url);

        mockMvc.perform(get("/internal/api/v1/urls/{shortCode}/access", "own1001")
                .param("userId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    @DisplayName("Should return 200 OK with allowed=false when user does not own the URL")
    void checkAccess_DifferentUser_ReturnsOkWithAllowedFalse() throws Exception {
        Url url = new Url();
        url.setShortCode("own1002");
        url.setOriginalUrl("https://example.com/owned2");
        url.setUserId(100L);
        url.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        urlRepository.save(url);

        mockMvc.perform(get("/internal/api/v1/urls/{shortCode}/access", "own1002")
                .param("userId", "200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    @DisplayName("Should return 200 OK with allowed=false when URL was created anonymously")
    void checkAccess_AnonymousUrl_ReturnsOkWithAllowedFalse() throws Exception {
        Url url = new Url();
        url.setShortCode("anon001");
        url.setOriginalUrl("https://example.com/anon");
        url.setUserId(null);
        url.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));
        urlRepository.save(url);

        mockMvc.perform(get("/internal/api/v1/urls/{shortCode}/access", "anon001")
                .param("userId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    @DisplayName("Should return 200 OK with allowed=false when shortCode does not exist")
    void checkAccess_UrlNotFound_ReturnsOkWithAllowedFalse() throws Exception {
        mockMvc.perform(get("/internal/api/v1/urls/{shortCode}/access", "none001")
                .param("userId", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allowed").value(false));
    }
}
