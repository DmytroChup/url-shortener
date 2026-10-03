package com.chupryna.url_shortener.scheduler;

import com.chupryna.url_shortener.entity.OutboxEvent;
import com.chupryna.url_shortener.entity.outbox.OutboxStatus;
import com.chupryna.url_shortener.event.KafkaTopics;
import com.chupryna.url_shortener.event.UrlClickEvent;
import com.chupryna.url_shortener.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPublisher Unit Tests")
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Test
    @DisplayName("Should publish pending event to Kafka and update status to SENT with timestamp")
    void publishPendingEvents_Success() {
        OutboxEvent event = createEvent(0, OutboxStatus.PENDING);
        UrlClickEvent clickEvent = createClickEvent();

        when(outboxEventRepository.findPendingEventsWithLock(50)).thenReturn(List.of(event));
        when(objectMapper.readValue(event.getPayload(), UrlClickEvent.class)).thenReturn(clickEvent);

        CompletableFuture<SendResult<String, UrlClickEvent>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(eq(KafkaTopics.URL_CLICKS), eq(event.getAggregateId()), eq(clickEvent)))
                .thenReturn(future);

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should increment retry count and keep PENDING status when Kafka send fails")
    void publishPendingEvents_KafkaError_IncrementsRetryCount() {
        OutboxEvent event = createEvent(0, OutboxStatus.PENDING);
        UrlClickEvent clickEvent = createClickEvent();

        when(outboxEventRepository.findPendingEventsWithLock(50)).thenReturn(List.of(event));
        when(objectMapper.readValue(event.getPayload(), UrlClickEvent.class)).thenReturn(clickEvent);

        CompletableFuture<SendResult<String, UrlClickEvent>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka broker unavailable"));

        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getSentAt()).isNull();
    }

    @Test
    @DisplayName("Should transition status to FAILED when max retries limit (3) is reached")
    void publishPendingEvents_MaxRetriesReached_TransitionsToFailed() {
        OutboxEvent event = createEvent(2, OutboxStatus.PENDING);
        UrlClickEvent clickEvent = createClickEvent();

        when(outboxEventRepository.findPendingEventsWithLock(50)).thenReturn(List.of(event));
        when(objectMapper.readValue(event.getPayload(), UrlClickEvent.class)).thenReturn(clickEvent);

        CompletableFuture<SendResult<String, UrlClickEvent>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Timeout waiting for ACK"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failedFuture);

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getSentAt()).isNull();
    }

    private OutboxEvent createEvent(int retryCount, OutboxStatus status) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("URL");
        event.setAggregateId("abc1234");
        event.setEventType("URL_CLICK");
        event.setPayload("{\"shortCode\":\"abc1234\"}");
        event.setStatus(status);
        event.setRetryCount(retryCount);
        event.setCreatedAt(Instant.now());
        return event;
    }

    private UrlClickEvent createClickEvent() {
        return new UrlClickEvent(
                UUID.randomUUID(),
                "abc1234",
                "Mozilla/5.0",
                "192.168.1.1",
                Instant.now(),
                "https://example.com"
        );
    }
}
