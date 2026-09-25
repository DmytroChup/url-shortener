package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.exception.LinkExpiredException;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.util.RandomShortCodeGenerator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private static final Duration CACHE_TTL = Duration.ofDays(1);
    static final Duration NEGATIVE_CACHE_TTL = Duration.ofSeconds(30);
    static final String CACHE_PREFIX = "url:";
    static final String NOT_FOUND_MARKER = "__NOT_FOUND__";

    private static final String EXPIRED_MARKER = "__EXPIRED__";
    private static final Duration EXPIRED_CACHE_TTL = Duration.ofMinutes(30);

    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:(//|[^0-9]).*");
    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{7}$");
    private static final int MAX_SAVE_RETRIES = 5;
    private static final Integer DEFAULT_TTL_DAYS = 30;

    private final UrlRepository urlRepository;
    private final RandomShortCodeGenerator codeGenerator;
    private final StringRedisTemplate redisTemplate;
    private final UrlPersister urlPersister;

    public String shortenUrl(String originalUrl, Integer ttlDays) {
        String normalizedUrl = normalizeUrl(originalUrl);
        validateUrl(normalizedUrl);

        Url savedUrl = saveWithRetryOnCollision(normalizedUrl, ttlDays);

        Duration ttl = calculateCacheTtl(savedUrl.getExpiresAt());
        redisTemplate.opsForValue().set(CACHE_PREFIX + savedUrl.getShortCode(), normalizedUrl, ttl);

        return savedUrl.getShortCode();
    }

    public String shortenUrl(String originalUrl) {
        return shortenUrl(originalUrl, null);
    }

    public String getOriginalUrl(String shortCode) {
        validateShortCode(shortCode);

        String cacheUrl = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);

        if(cacheUrl != null) {
            if (NOT_FOUND_MARKER.equals(cacheUrl)) {
                throw new EntityNotFoundException("Url not found");
            } else if(EXPIRED_MARKER.equals(cacheUrl)) {
                throw new LinkExpiredException("This url has been expired");
            }
            return cacheUrl;
        }

        Optional<Url> urlOptional = urlRepository.findByShortCode(shortCode);

        if (urlOptional.isEmpty()) {
            redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, NOT_FOUND_MARKER, NEGATIVE_CACHE_TTL);
            throw new EntityNotFoundException("Url not found");
        }

        if(urlOptional.get().getExpiresAt() != null && urlOptional.get().getExpiresAt().isBefore(Instant.now())) {
            redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, EXPIRED_MARKER, EXPIRED_CACHE_TTL);
            throw new LinkExpiredException("This link has been expired");
        }

        String originalUrl = urlOptional.get().getOriginalUrl();

        Duration ttl = calculateCacheTtl(urlOptional.get().getExpiresAt());
        redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, originalUrl, ttl);

        return originalUrl;
    }

    private void validateShortCode(String shortCode) {
        if (shortCode == null || !SHORT_CODE_PATTERN.matcher(shortCode).matches()) {
            throw new IllegalArgumentException("Invalid short code format");
        }
    }

    private Url saveWithRetryOnCollision(String normalizedUrl, Integer ttlDays) {
        for (int attempt = 0; attempt < MAX_SAVE_RETRIES; attempt++) {
            String candidateCode = codeGenerator.generate();

            Url url = new Url();
            url.setShortCode(candidateCode);
            url.setOriginalUrl(normalizedUrl);
            url.setExpiresAt(ttlDays == null ?
                    Instant.now().plus(Duration.ofDays(DEFAULT_TTL_DAYS)) :
                    Instant.now().plus(Duration.ofDays(ttlDays)));

            try {
                return urlPersister.persist(url);
            } catch (DataIntegrityViolationException e) {
                log.warn("Short code collision for code: {}, retrying (attempt {}/{})",
                        candidateCode, attempt + 1, MAX_SAVE_RETRIES);
            }
        }

        throw new IllegalStateException(
                "Failed to generate unique short code after " + MAX_SAVE_RETRIES + " attempts");
    }

    private String normalizeUrl(String originalUrl) {
        String trimmedUrl = originalUrl.trim();

        if (SCHEME_PATTERN.matcher(trimmedUrl).matches()) {
            return trimmedUrl;
        }

        return "https://" + trimmedUrl;
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || (!uri.getScheme().equalsIgnoreCase("https")
                    && !uri.getScheme().equalsIgnoreCase("http"))) {
                throw new IllegalArgumentException("Invalid URL format");
            }
            if(uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Invalid URL format");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
    }

    private Duration calculateCacheTtl(Instant expiresAt) {
        if(expiresAt == null) {
            return CACHE_TTL;
        }

        Duration remaining =  Duration.between(Instant.now(), expiresAt);

        if(remaining.isNegative() || remaining.isZero()) {
            return Duration.ZERO;
        }

        return remaining.compareTo(CACHE_TTL) < 0 ? remaining : CACHE_TTL;
    }
}
