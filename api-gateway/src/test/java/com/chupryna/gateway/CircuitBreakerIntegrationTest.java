package com.chupryna.gateway;

import com.chupryna.gateway.util.TestJwt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@DisplayName("CircuitBreaker Integration Tests")
class CircuitBreakerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should route to fallback and return 503 when downstream analytics service fails with 500")
    void getAnalytics_DownstreamFails_Returns503Fallback() {
        wireMockServer.stubFor(get(urlPathMatching("/api/v1/analytics/.*"))
                .willReturn(serverError()));

        String token = TestJwt.token(jwtProperties.secret(), 42L, "ROLE_USER", Duration.ofMinutes(5));

        webTestClient.get()
                .uri("/api/v1/analytics/abc1234")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.title").isEqualTo("Service Unavailable")
                .jsonPath("$.detail").isEqualTo(
                        "Analytics service is temporarily unavailable. Please try again later.");
    }
}
