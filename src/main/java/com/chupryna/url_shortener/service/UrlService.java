package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.repository.UrlRepository;
import com.chupryna.url_shortener.util.Base62Encoder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;

    public String shortenUrl(String originalUrl) {
        String normalizedUrl = normalizeUrl(originalUrl);

        Url url = new Url();
        url.setOriginalUrl(normalizedUrl);

        return base62Encoder.encode(urlRepository.save(url).getId());
    }

    public String getOriginalUrl(String shortCode) {
        long id = base62Encoder.decode(shortCode);

        return urlRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Url not found"))
                .getOriginalUrl();
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
