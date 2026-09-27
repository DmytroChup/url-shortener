package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.dto.UrlAnalyticsResponse;
import com.chupryna.url_shortener.repository.UrlClickRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlAnalyticsService Unit Tests")
class UrlAnalyticsServiceTest {

    @Mock
    private UrlService urlService;

    @Mock
    private UrlClickRepository urlClickRepository;

    @InjectMocks
    private UrlAnalyticsService urlAnalyticsService;

    @Test
    @DisplayName("Should return analytics with original URL and total click count")
    void getAnalytics_Success() {
        String shortCode = "aB7xK9q";
        String originalUrl = "https://example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);
        when(urlClickRepository.countByShortCode(shortCode)).thenReturn(42L);

        UrlAnalyticsResponse response = urlAnalyticsService.getAnalytics(shortCode);

        assertEquals(shortCode, response.shortCode());
        assertEquals(originalUrl, response.originalUrl());
        assertEquals(42L, response.totalClicks());
    }

    @Test
    @DisplayName("Should return zero clicks when URL exists but has never been visited")
    void getAnalytics_NoClicksYet() {
        String shortCode = "aB7xK9q";
        String originalUrl = "https://example.com";

        when(urlService.getOriginalUrl(shortCode)).thenReturn(originalUrl);
        when(urlClickRepository.countByShortCode(shortCode)).thenReturn(0L);

        UrlAnalyticsResponse response = urlAnalyticsService.getAnalytics(shortCode);

        assertEquals(0L, response.totalClicks());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException and NEVER query click count when short code does not exist")
    void getAnalytics_ShortCodeNotFound_ThrowsAndSkipsClickCountQuery() {
        String shortCode = "unknown";

        when(urlService.getOriginalUrl(shortCode))
                .thenThrow(new EntityNotFoundException("Url not found"));

        assertThrows(EntityNotFoundException.class, () -> urlAnalyticsService.getAnalytics(shortCode));

        verify(urlClickRepository, never()).countByShortCode(any());
    }
}