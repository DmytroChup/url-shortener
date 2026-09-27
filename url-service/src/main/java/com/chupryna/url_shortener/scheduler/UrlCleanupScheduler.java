package com.chupryna.url_shortener.scheduler;

import com.chupryna.url_shortener.repository.UrlClickRepository;
import com.chupryna.url_shortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrlCleanupScheduler {

    private final UrlRepository urlRepository;
    private final UrlClickRepository urlClickRepository;

    @Value("${app.cleanup.grace-period-days:30}")
    private long gracePeriodDays;

    @Scheduled(cron = "${app.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void purgeExpiredLinks() {
        Instant cutoff = Instant.now().minus(gracePeriodDays, ChronoUnit.DAYS);

        List<String> expiredCodes = urlRepository.findShortCodesExpiredBefore(cutoff);

        if (expiredCodes.isEmpty()) {
            log.info("No expired links older than {} days found", gracePeriodDays);
            return;
        }

        int deletedClicks = urlClickRepository.deleteByShortCodeIn(expiredCodes);
        int deletedUrls = urlRepository.deleteByExpiresAtBefore(cutoff);

        log.info("Purged {} expired links (older than {} days) and {} associated click records",
                deletedUrls, gracePeriodDays, deletedClicks);
    }

}
