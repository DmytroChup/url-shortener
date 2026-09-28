package com.chupryna.analytics.service;

import com.chupryna.analytics.dto.UrlAnalyticsResponse;
import com.chupryna.analytics.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrlAnalyticsService {

    private final UrlClickRepository urlClickRepository;

    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        long totalClicks = urlClickRepository.countByShortCode(shortCode);
        return new UrlAnalyticsResponse(shortCode, totalClicks);
    }
}
