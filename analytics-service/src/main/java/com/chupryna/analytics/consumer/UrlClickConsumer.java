package com.chupryna.analytics.consumer;

import com.chupryna.analytics.entity.UrlClick;
import com.chupryna.analytics.repository.UrlClickRepository;
import com.chupryna.url_shortener.event.KafkaTopics;
import com.chupryna.url_shortener.event.UrlClickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UrlClickConsumer {

    private static final int MAX_REFERER_LENGTH = 2048;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final UrlClickRepository urlClickRepository;

    @KafkaListener(topics = KafkaTopics.URL_CLICKS, groupId = "analytics-group")
    public void consume(UrlClickEvent event) {
        if (event.eventId() == null) {
            log.error("Received click event without eventId for shortCode: {}", event.shortCode());
            throw new IllegalArgumentException("eventId is required for idempotent processing");
        }

        log.info("Received click event for shortCode: {}, eventId: {}", event.shortCode(), event.eventId());

        if (urlClickRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate click event (eventId={}), skipping (fast-path)", event.eventId());
            return;
        }

        UrlClick urlClick = UrlClick.builder()
                .eventId(event.eventId())
                .shortCode(event.shortCode())
                .userAgent(truncate(event.userAgent(), MAX_USER_AGENT_LENGTH))
                .maskedIpAddress(event.maskedIpAddress())
                .clickedAt(event.clickedAt() != null ? event.clickedAt() : Instant.now())
                .referer(truncate(event.referer(), MAX_REFERER_LENGTH))
                .build();

        try {
            urlClickRepository.save(urlClick);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate click event (eventId={}), safely ignored by DB constraint", event.eventId());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
