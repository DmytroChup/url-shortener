package com.chupryna.analytics.service;

import com.chupryna.analytics.dto.UrlAnalyticsResponse;
import com.chupryna.analytics.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrlAnalyticsService {

    private static final Pageable TOP_10 = PageRequest.of(0, 10);

    private final UrlClickRepository urlClickRepository;

    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        long totalClicks = urlClickRepository.countByShortCode(shortCode);

        Map<String, Long> topCountries = toMap(urlClickRepository.findTopCountries(shortCode, TOP_10));
        Map<String, Long> devices = toMap(urlClickRepository.findDevices(shortCode, TOP_10));
        Map<String, Long> topBrowsers = toMap(urlClickRepository.findTopBrowsers(shortCode, TOP_10));
        Map<String, Long> topOperatingSystems = toMap(urlClickRepository.findTopOperatingSystems(shortCode, TOP_10));

        return new UrlAnalyticsResponse(shortCode, totalClicks, topCountries, devices, topBrowsers,
                topOperatingSystems);
    }

    private Map<String, Long> toMap(List<UrlClickRepository.MetricCount> metrics) {
        return metrics.stream()
                .filter(m -> m.getName() != null)
                .collect(Collectors.toMap(
                        UrlClickRepository.MetricCount::getName,
                        UrlClickRepository.MetricCount::getTotal,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
