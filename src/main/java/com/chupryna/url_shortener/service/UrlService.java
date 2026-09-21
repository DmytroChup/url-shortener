package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final String CACHE_PREFIX = "url:";
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:(//|[^0-9]).*");
    private static final int MAX_SAVE_RETRIES = 5;

    private final UrlRepository urlRepository;
    private final RandomShortCodeGenerator codeGenerator;
    private final StringRedisTemplate redisTemplate;
    private final UrlPersister urlPersister;

    public String shortenUrl(String originalUrl) {
        String normalizedUrl = normalizeUrl(originalUrl);
        validateUrl(normalizedUrl);

        Url savedUrl = saveWithRetryOnCollision(normalizedUrl);

        redisTemplate.opsForValue().set(CACHE_PREFIX + savedUrl.getShortCode(), normalizedUrl, CACHE_TTL);

        return savedUrl.getShortCode();
    }

    public String getOriginalUrl(String shortCode) {
        String cacheUrl = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);

        if(cacheUrl != null) {
            return cacheUrl;
        }

        String originalUrl = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new EntityNotFoundException("Url not found"))
                .getOriginalUrl();

        redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, originalUrl, CACHE_TTL);

        return originalUrl;
    }

    private Url saveWithRetryOnCollision(String normalizedUrl) {
        for (int attempt = 0; attempt < MAX_SAVE_RETRIES; attempt++) {
            String candidateCode = codeGenerator.generate();

            Url url = new Url();
            url.setShortCode(candidateCode);
            url.setOriginalUrl(normalizedUrl);

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
}
