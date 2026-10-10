package com.chupryna.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT10S")
public class CircuitBreakerIntegrationTest {

    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.8.3-alpine"))
            .withExposedPorts(6379);

    static WireMockServer wireMockServer;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startAll() {
        redis.start();

        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ANALYTICS_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    @DisplayName("Should route to fallback and return 503 when downstream analytics service fails with 500")
    void getAnalytics_DownstreamFails_Returns503Fallback() {
        wireMockServer.stubFor(get(urlPathMatching("/api/v1/analytics/.*"))
                .willReturn(serverError()));

        webTestClient.get()
                .uri("/api/v1/analytics/abc1234")
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
