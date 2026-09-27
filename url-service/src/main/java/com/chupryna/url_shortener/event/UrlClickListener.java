package com.chupryna.url_shortener.event;

import com.chupryna.url_shortener.entity.UrlClick;
import com.chupryna.url_shortener.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrlClickListener {

    private static final int MAX_REFERER_LENGTH = 2048;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final UrlClickRepository urlClickRepository;

    @Async("taskExecutor")
    @EventListener
    public void handleUrlClickEvent(UrlClickEvent event) {
        UrlClick urlClick = UrlClick.builder()
                .shortCode(event.shortCode())
                .userAgent(truncate(event.userAgent(), MAX_USER_AGENT_LENGTH))
                .maskedIpAddress(event.maskedIpAddress())
                .clickedAt(event.clickedAt() != null ? event.clickedAt() : Instant.now())
                .referer(truncate(event.referer(), MAX_REFERER_LENGTH))
                .build();

        try {
            urlClickRepository.save(urlClick);
        } catch (Exception e) {
            log.error("Failed to record click analytics for shortCode: {}", event.shortCode(), e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
