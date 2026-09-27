package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.dto.UrlAnalyticsResponse;
import com.chupryna.url_shortener.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlAnalyticsService {

    private final UrlClickRepository urlClickRepository;
    private final UrlService urlService;

    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        long clicks = urlClickRepository.countByShortCode(shortCode);
        return new UrlAnalyticsResponse(shortCode, originalUrl, clicks);
    }
}
