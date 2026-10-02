package com.chupryna.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT10S")
class RateLimiterIntegrationTest {

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
        wireMockServer.stubFor(post(urlPathMatching("/api/v1/urls/.*"))
                .willReturn(okJson("{\"status\":\"ok\"}")));
    }

    @AfterAll
    static void stopAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("URL_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    void shouldRateLimitAfterBurstCapacityExceeded() {
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
