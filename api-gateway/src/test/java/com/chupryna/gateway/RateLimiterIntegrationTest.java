package com.chupryna.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiter Integration Tests")
class RateLimiterIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should rate limit after burst capacity exceeded")
    void rateLimit_BurstCapacityExceeded_Returns429TooManyRequests() {
        wireMockServer.stubFor(post(urlPathMatching("/api/v1/urls/.*"))
                .willReturn(okJson("{\"status\":\"ok\"}")));

        int maxAttempts = 30;
        int successful = 0;
        boolean limited = false;

        for (int i = 0; i < maxAttempts; i++) {
            var status = webTestClient.post()
                    .uri("/api/v1/urls/shorten")
                    .exchange()
                    .returnResult(String.class)
                    .getStatus();

            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                limited = true;
                break;
            }
            successful++;
        }

        assertThat(limited).as("The limit should have triggered.").isTrue();
        assertThat(successful).as("burstCapacity must not be exhausted too early.").isGreaterThanOrEqualTo(10);
    }
}
