package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.OutboxEvent;
import com.chupryna.url_shortener.entity.outbox.OutboxStatus;
import com.chupryna.url_shortener.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType(aggregateType);
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(jsonPayload);
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setCreatedAt(Instant.now());

            outboxEventRepository.save(outboxEvent);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Failed to serialize event payload to JSON", e);
        }
    }
}
