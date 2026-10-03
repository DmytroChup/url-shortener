package com.chupryna.url_shortener.scheduler;

import com.chupryna.url_shortener.entity.OutboxEvent;
import com.chupryna.url_shortener.entity.outbox.OutboxStatus;
import com.chupryna.url_shortener.event.KafkaTopics;
import com.chupryna.url_shortener.event.UrlClickEvent;
import com.chupryna.url_shortener.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEventsWithLock(50);

        for (OutboxEvent event : events) {
            try {
                UrlClickEvent clickEvent = objectMapper.readValue(event.getPayload(), UrlClickEvent.class);

                kafkaTemplate.send(KafkaTopics.URL_CLICKS, event.getAggregateId(), clickEvent)
                        .get(5, TimeUnit.SECONDS);

                event.setStatus(OutboxStatus.SENT);
                event.setSentAt(Instant.now());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
        }
    }
}
