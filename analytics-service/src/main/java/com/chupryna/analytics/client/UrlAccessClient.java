package com.chupryna.analytics.client;

import com.chupryna.analytics.dto.UrlAccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlAccessClient {

    private final RestClient urlServiceRestClient;

    @Cacheable(value = "url-access", key = "#shortCode + ':' + #userId")
    public boolean hasAccess(String shortCode, Long userId) {
        try {
            UrlAccessResponse response = urlServiceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/internal/api/v1/urls/{shortCode}/access")
                    .queryParam("userId", userId)
                    .build(shortCode))
                .retrieve()
                .body(UrlAccessResponse.class);
            return response != null && response.allowed();
        } catch (RestClientException ex) {
            log.warn("Failed to check URL access for shortCode={} and userId={}: {}", shortCode, userId, ex.getMessage());
            return false;
        }
    }
}
