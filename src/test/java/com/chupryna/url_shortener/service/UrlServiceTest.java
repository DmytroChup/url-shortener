package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.util.RandomShortCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final String CACHE_PREFIX = "url:";

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RandomShortCodeGenerator codeGenerator;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should return URL from Redis cache without querying database on cache hit")
    void getOriginalUrl_CacheHit() {
        String shortCode = "aB7xK9q";
        String originalUrl = "https://example.com";
        when(valueOperations.get(CACHE_PREFIX + shortCode)).thenReturn(originalUrl);

        String actualUrl = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, actualUrl);
        verify(urlRepository, never()).findByShortCode(any());
    }

    @Test
    @DisplayName("Should fetch URL from database and populate Redis cache on cache miss")
    void getOriginalUrl_CacheMiss() {
        String shortCode = "aB7xK9q";
        String originalUrl = "https://example.com";

        Url entity = new Url();
        entity.setId(1L);
        entity.setShortCode(shortCode);
        entity.setOriginalUrl(originalUrl);

        when(valueOperations.get(CACHE_PREFIX + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(entity));

        String actualUrl = urlService.getOriginalUrl(shortCode);

        assertEquals(originalUrl, actualUrl);

        verify(valueOperations).set(eq(CACHE_PREFIX + shortCode), eq(originalUrl), any(Duration.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when short code does not exist in cache or database")
    void getOriginalUrl_NotFound() {
        String shortCode = "unknown";

        when(valueOperations.get(CACHE_PREFIX + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> urlService.getOriginalUrl(shortCode));

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("Should normalize URL, persist entity, generate Base62 code, and warm up cache")
    void shortenUrl_Success() {
        String shortCode = "aB7xK9q";
        String badUrl = "example.com";
        String normalizedUrl = "https://example.com";

        Url savedEntity = new Url();
        savedEntity.setId(1L);
        savedEntity.setShortCode(shortCode);
        savedEntity.setOriginalUrl(normalizedUrl);

        when(codeGenerator.generate()).thenReturn(shortCode);
        when(urlRepository.existsByShortCode(shortCode)).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(savedEntity);

        String foundShortCode = urlService.shortenUrl(badUrl);

        assertEquals(shortCode, foundShortCode);
        verify(urlRepository).save(argThat(url -> normalizedUrl.equals(url.getOriginalUrl()) &&
                shortCode.equals(url.getShortCode())));
        verify(valueOperations).set(eq(CACHE_PREFIX + shortCode), eq(normalizedUrl), any(Duration.class));
    }

    @ParameterizedTest
    @CsvSource({
            "google.com,           https://google.com",
            "localhost:8080,       https://localhost:8080",
            "example.com:8080/x,   https://example.com:8080/x",
            "http://example.com,   http://example.com",
            "HTTPS://Example.com,  HTTPS://Example.com"
    })
    @DisplayName("Should normalize URL correctly before persisting and caching")
    void shortenUrl_NormalizesUrlCorrectly(String inputUrl, String expectedNormalizedUrl) {
        String shortCode = "aB7xK9q";

        when(codeGenerator.generate()).thenReturn(shortCode);
        when(urlRepository.existsByShortCode(shortCode)).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> {
            Url url = inv.getArgument(0);
            url.setId(1L);
            return url;
        });

        urlService.shortenUrl(inputUrl);

        verify(urlRepository).save(argThat(url ->
                expectedNormalizedUrl.equals(url.getOriginalUrl())));
        verify(valueOperations).set(eq(CACHE_PREFIX + shortCode), eq(expectedNormalizedUrl), any(Duration.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com",
            "mailto:admin@example.com",
            "tel:+12345",
            "javascript:alert(1)",
            "http:example.com",
            "https://",
            "http://",
            "https://   "
    })
    @DisplayName("Should throw IllegalArgumentException and NOT touch database or cache when URL is invalid")
    void shortenUrl_InvalidUrl_ThrowsExceptionAndNeverPersists(String invalidUrl) {
        assertThrows(IllegalArgumentException.class, () -> urlService.shortenUrl(invalidUrl));

        verify(urlRepository, never()).save(any());
        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }
}
