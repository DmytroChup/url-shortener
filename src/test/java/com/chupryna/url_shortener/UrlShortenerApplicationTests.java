package com.chupryna.url_shortener;

import com.chupryna.url_shortener.dto.UrlRequest;
import com.chupryna.url_shortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void shouldShortenUrlAndRedirectSuccessfully() throws Exception {
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
    void shouldReturnNotFoundWhenShortCodeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/notfound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Url not found"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body has empty URL")
    void shouldReturnBadRequestWhenUrlIsInvalid() throws Exception {
        UrlRequest invalidRequest = new UrlRequest("");

        mockMvc.perform(post("/api/v1/shorten")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").value("URL cannot be empty"));
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit is exceeded")
    void shouldReturnTooManyRequestsWhenRateLimitExceeded() throws Exception {
        String clientIp = "192.168.1.100";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/test").header("X-Forwarded-For", clientIp));
        }

        mockMvc.perform(get("/api/v1/test").header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }
}
