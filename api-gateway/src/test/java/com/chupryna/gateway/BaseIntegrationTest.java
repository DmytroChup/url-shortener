package com.chupryna.gateway;

import com.chupryna.gateway.config.JwtProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT10S")
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @ServiceConnection(name = "redis")
    protected static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.8.3-alpine"))
            .withExposedPorts(6379);

    protected static final WireMockServer wireMockServer;

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected JwtProperties jwtProperties;

    @Autowired
    protected ReactiveStringRedisTemplate redisTemplate;

    static {
        redis.start();
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("URL_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
        registry.add("ANALYTICS_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
        registry.add("AUTH_SERVICE_URL", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void resetState() {
        wireMockServer.resetAll();
        var connection = redisTemplate.getConnectionFactory().getReactiveConnection();
        connection.serverCommands().flushAll().block();
    }
}
