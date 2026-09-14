package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.util.Base62Encoder;
import com.chupryna.url_shortener.util.IdObfuscator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:(//|[^0-9]).*");

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final StringRedisTemplate redisTemplate;
    private final IdObfuscator idObfuscator;

    public String shortenUrl(String originalUrl) {
        String normalizedUrl = normalizeUrl(originalUrl);
        validateUrl(normalizedUrl);

        Url url = new Url();
        url.setOriginalUrl(normalizedUrl);

        Url savedUrl = urlRepository.save(url);

        long obfuscatedId = idObfuscator.obfuscate(savedUrl.getId());
        String shortCode = base62Encoder.encode(obfuscatedId);

        redisTemplate.opsForValue().set(shortCode, normalizedUrl, CACHE_TTL);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        String cacheUrl = redisTemplate.opsForValue().get(shortCode);

        if(cacheUrl != null) {
            return cacheUrl;
        }

        long obfuscatedId = base62Encoder.decode(shortCode);
        long id = idObfuscator.deobfuscate(obfuscatedId);

        String originalUrl = urlRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Url not found"))
                .getOriginalUrl();

        redisTemplate.opsForValue().set(shortCode, originalUrl, CACHE_TTL);

        return originalUrl;
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
