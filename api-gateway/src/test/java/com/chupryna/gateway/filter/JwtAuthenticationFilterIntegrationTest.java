package com.chupryna.gateway.filter;

import com.chupryna.gateway.BaseIntegrationTest;
import com.chupryna.gateway.util.TestJwt;
import com.chupryna.url_shortener.security.SecurityHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@DisplayName("JwtAuthenticationFilter Integration Tests")
class JwtAuthenticationFilterIntegrationTest extends BaseIntegrationTest {

    private static final String FOREIGN_SECRET = "1111111122222222333333334444444455555555666666667777777788888888";

    @Test
    @DisplayName("Should return 401 Unauthorized when requesting protected path without token")
    void protectedPath_NoToken_Returns401() {
        webTestClient.get()
            .uri("/api/v1/urls/me")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when requesting protected path with expired token")
    void protectedPath_ExpiredToken_Returns401() {
        String expiredToken = TestJwt.token(jwtProperties.secret(), 42L, "ROLE_USER", Duration.ofMinutes(-1));

        webTestClient.get()
            .uri("/api/v1/urls/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when token signature was signed with another secret")
    void protectedPath_ForeignSecretToken_Returns401() {
        String forgedToken = TestJwt.token(FOREIGN_SECRET, 42L, "ROLE_USER", Duration.ofMinutes(5));

        webTestClient.get()
            .uri("/api/v1/urls/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Should forward validated X-User-Id and X-User-Role headers when valid JWT is provided")
    void protectedPath_ValidToken_ForwardsUserHeaders() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/urls/me"))
            .willReturn(okJson("[]")));

        String validToken = TestJwt.token(jwtProperties.secret(), 42L, "ROLE_USER", Duration.ofMinutes(5));

        webTestClient.get()
            .uri("/api/v1/urls/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
            .exchange()
            .expectStatus().isOk();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/urls/me"))
            .withHeader(SecurityHeaders.X_USER_ID, equalTo("42"))
            .withHeader(SecurityHeaders.X_USER_ROLE, equalTo("ROLE_USER")));
    }

    @Test
    @DisplayName("Should unconditionally strip spoofed X-User-Id headers on guest shorten requests")
    void guestShorten_SpoofedHeaders_AreStripped() {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/v1/urls/shorten"))
            .willReturn(ok("aB7xK9q")));

        webTestClient.post()
            .uri("/api/v1/urls/shorten")
            .header(SecurityHeaders.X_USER_ID, "999")
            .header(SecurityHeaders.X_USER_ROLE, "ROLE_ADMIN")
            .exchange()
            .expectStatus().isOk();

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/v1/urls/shorten"))
            .withoutHeader(SecurityHeaders.X_USER_ID)
            .withoutHeader(SecurityHeaders.X_USER_ROLE));
    }

    @Test
    @DisplayName("Should treat request as guest when expired token is sent on guest shorten endpoint")
    void guestShorten_ExpiredToken_IsTreatedAsGuest() {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/v1/urls/shorten"))
            .willReturn(ok("aB7xK9q")));

        String expiredToken = TestJwt.token(jwtProperties.secret(), 42L, "ROLE_USER", Duration.ofMinutes(-1));

        webTestClient.post()
            .uri("/api/v1/urls/shorten")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
            .exchange()
            .expectStatus().isOk();

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/v1/urls/shorten"))
            .withoutHeader(SecurityHeaders.X_USER_ID));
    }

    @Test
    @DisplayName("Should allow public redirect without token")
    void publicRedirect_NoToken_PassesThrough() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/urls/aB7xK9q"))
            .willReturn(status(302).withHeader(HttpHeaders.LOCATION, "https://example.com")));

        webTestClient.get()
            .uri("/aB7xK9q")
            .exchange()
            .expectStatus().isFound();

        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/api/v1/urls/aB7xK9q"))
            .withoutHeader(SecurityHeaders.X_USER_ID));
    }

    @Test
    @DisplayName("Should allow public auth endpoints to proceed even if expired access token is attached")
    void publicAuth_ExpiredToken_PassesThrough() {
        wireMockServer.stubFor(post(urlPathEqualTo("/api/v1/auth/refresh"))
            .willReturn(okJson("{\"status\":\"ok\"}")));

        String expiredToken = TestJwt.token(jwtProperties.secret(), 42L, "ROLE_USER", Duration.ofMinutes(-1));

        webTestClient.post()
            .uri("/api/v1/auth/refresh")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
            .exchange()
            .expectStatus().isOk();

        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/api/v1/auth/refresh"))
            .withoutHeader(SecurityHeaders.X_USER_ID));
    }
}
