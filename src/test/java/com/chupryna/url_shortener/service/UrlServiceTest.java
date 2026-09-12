package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.util.Base62Encoder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Unit Tests")
public class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should return URL from Redis cache without querying database on cache hit")
    void getOriginalUrl_CacheHit() {
        String shortCode = "b";
        String originalUrl = "https://example.com";
        when(valueOperations.get(shortCode)).thenReturn(originalUrl);

        String actualUrl = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, actualUrl);
        verify(urlRepository, never()).findById(any());

    }

    @Test
    @DisplayName("Should fetch URL from database and populate Redis cache on cache miss")
    void getOriginalUrl_CacheMiss() {
        String shortCode = "b";
        String originalUrl = "https://example.com";
        when(valueOperations.get(shortCode)).thenReturn(null);
        when(base62Encoder.decode(shortCode)).thenReturn(1L);

        Url entity = new Url();
        entity.setId(1L);
        entity.setOriginalUrl(originalUrl);

        when(urlRepository.findById(1L)).thenReturn(Optional.of(entity));

        String actualUrl = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, actualUrl);

        verify(valueOperations).set(eq(shortCode), eq(originalUrl), any(Duration.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when short code does not exist in cache or database")
    void getOriginalUrl_NotFound() {
        String shortCode = "b";

        when(valueOperations.get(shortCode)).thenReturn(null);
        when(base62Encoder.decode(shortCode)).thenReturn(999L);
        when(urlRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> urlService.getOriginalUrl(shortCode));

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("Should normalize URL, persist entity, generate Base62 code, and warm up cache")
    void shortenUrl_Success() {
        String shortCode = "b";
        String badUrl = "example.com";
        String normalizedUrl = "https://example.com";

        Url savedEntity = new Url();
        savedEntity.setId(1L);
        savedEntity.setOriginalUrl(normalizedUrl);

        when(urlRepository.save(any(Url.class))).thenReturn(savedEntity);
        when(base62Encoder.encode(1L)).thenReturn(shortCode);

        String foundShortCode = urlService.shortenUrl(badUrl);

        assertEquals(shortCode, foundShortCode);
        verify(urlRepository).save(argThat(url -> normalizedUrl.equals(url.getOriginalUrl())));
        verify(valueOperations).set(eq(shortCode), eq(normalizedUrl), any(Duration.class));
    }
}
