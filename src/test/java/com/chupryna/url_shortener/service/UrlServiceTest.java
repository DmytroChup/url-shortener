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
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final String NOT_FOUND_MARKER = "__NOT_FOUND__";
    private static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RandomShortCodeGenerator codeGenerator;

    @Mock
    private UrlPersister urlPersister;

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

        verify(valueOperations).set(eq(CACHE_PREFIX + shortCode), eq(NOT_FOUND_MARKER), eq(NEGATIVE_CACHE_TTL));
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
        when(urlPersister.persist(any(Url.class))).thenReturn(savedEntity);

        String foundShortCode = urlService.shortenUrl(badUrl);

        assertEquals(shortCode, foundShortCode);
        verify(urlPersister).persist(argThat(url -> normalizedUrl.equals(url.getOriginalUrl()) &&
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
        when(urlPersister.persist(any(Url.class))).thenAnswer(inv -> {
            Url url = inv.getArgument(0);
            url.setId(1L);
            return url;
        });

        urlService.shortenUrl(inputUrl);

        verify(urlPersister).persist(argThat(url ->
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

    @Test
    @DisplayName("Should retry with a new short code and succeed when collision occurs")
    void shortenUrl_Collision_RetriesAndSucceeds() {
        String originalUrl = "https://example.com";
        String firstCode = "first12";
        String secondCode = "second9";

        when(codeGenerator.generate()).thenReturn(firstCode, secondCode);

        Url savedEntity = new Url();
        savedEntity.setId(1L);
        savedEntity.setShortCode(secondCode);
        savedEntity.setOriginalUrl(originalUrl);

        when(urlPersister.persist(any(Url.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"))
                .thenReturn(savedEntity);

        String result = urlService.shortenUrl(originalUrl);

        assertEquals(secondCode, result);

        verify(codeGenerator, times(2)).generate();
        verify(urlPersister, times(2)).persist(any(Url.class));

        verify(valueOperations).set(eq(CACHE_PREFIX + secondCode), eq(originalUrl), any(Duration.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when max collision retries are exhausted")
    void shortenUrl_MaxRetriesExhausted_ThrowsIllegalStateException() {
        String normalizedUrl = "https://example.com";
        String shortCode = "dummyCode";

        when(codeGenerator.generate()).thenReturn(shortCode);
        when(urlPersister.persist(any(Url.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        assertThrows(IllegalStateException.class, () -> urlService.shortenUrl(normalizedUrl));

        verify(codeGenerator, times(5)).generate();
        verify(urlPersister, times(5)).persist(any(Url.class));

        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException immediately when negative cache marker is present in Redis")
    void getOriginalUrl_NegativeCacheHit() {
        String shortCode = "unknown";
        when(valueOperations.get(CACHE_PREFIX + shortCode)).thenReturn(NOT_FOUND_MARKER);

        assertThrows(EntityNotFoundException.class, () -> urlService.getOriginalUrl(shortCode));

        verify(urlRepository, never()).findByShortCode(any());
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }
}
