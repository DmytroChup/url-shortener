package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.util.Base62Encoder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final StringRedisTemplate redisTemplate;

    public String shortenUrl(String originalUrl) {
        String normalizedUrl = normalizeUrl(originalUrl);

        Url url = new Url();
        url.setOriginalUrl(normalizedUrl);

        Url savedUrl = urlRepository.save(url);
        String shortCode = base62Encoder.encode(savedUrl.getId());

        redisTemplate.opsForValue().set(shortCode, normalizedUrl, CACHE_TTL);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {
        String cacheUrl = redisTemplate.opsForValue().get(shortCode);

        if(cacheUrl != null) {
            return cacheUrl;
        }

        long id = base62Encoder.decode(shortCode);
        String originalUrl = urlRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Url not found"))
                .getOriginalUrl();

        redisTemplate.opsForValue().set(shortCode, originalUrl, CACHE_TTL);

        return originalUrl;
    }

    private String normalizeUrl(String originalUrl) {
        String trimmedUrl = originalUrl.trim();
        String lower = trimmedUrl.toLowerCase();

        if(Stream.of("http://", "https://").noneMatch(lower::startsWith)) {
            return "https://" + trimmedUrl;
        }

        return trimmedUrl;
    }
}
