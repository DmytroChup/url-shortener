package com.chupryna.analytics;

import com.chupryna.analytics.repository.UrlClickRepository;
import com.chupryna.url_shortener.event.KafkaTopics;
import com.chupryna.url_shortener.event.UrlClickEvent;
import com.chupryna.url_shortener.security.SecurityHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsApplicationTests extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UrlClickRepository urlClickRepository;

    private final BlockingQueue<ConsumerRecord<String, UrlClickEvent>> dltRecords = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() {
        urlClickRepository.deleteAll();
        dltRecords.clear();
    }

    @KafkaListener(topics = KafkaTopics.URL_CLICKS_DLT, groupId = "dlt-test-group")
    void consumeDlt(ConsumerRecord<String, UrlClickEvent> record) {
        dltRecords.add(record);
    }

    @Test
    @DisplayName("Should route poison pill event to DLT and continue processing subsequent valid events")
    void consume_PoisonPillEvent_RoutesToDltWithoutBlockingPartition() throws Exception {
        String poisonCode = "poison1";

        UrlClickEvent poisonPill = new UrlClickEvent(
                null,
                poisonCode,
                "Mozilla/5.0",
                "192.168.1.1",
                Instant.now(),
                "https://google.com"
        );

        kafkaTemplate.send(KafkaTopics.URL_CLICKS, poisonCode, poisonPill);

        ConsumerRecord<String, UrlClickEvent> dltRecord = dltRecords.poll(10, TimeUnit.SECONDS);
        assertThat(dltRecord).isNotNull();
        assertThat(dltRecord.value().shortCode()).isEqualTo(poisonCode);

        assertThat(urlClickRepository.countByShortCode(poisonCode)).isZero();

        String validCode = "valid01";
        UrlClickEvent validEvent = new UrlClickEvent(
                UUID.randomUUID(),
                validCode,
                "Mozilla/5.0",
                "192.168.1.1",
                Instant.now(),
                "https://google.com"
        );
        kafkaTemplate.send(KafkaTopics.URL_CLICKS, validCode, validEvent);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = urlClickRepository.countByShortCode(validCode);
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Should consume click events from Kafka, persist to DB, and return aggregated metrics via REST")
    void consume_ValidClickEvents_PersistsAndExposesMetrics() throws Exception {
        String shortCode = "abc1234";

        for (int i = 0; i < 3; i++) {
            UrlClickEvent event = new UrlClickEvent(
                    UUID.randomUUID(),
                    shortCode,
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
                    "192.168.1.1",
                    Instant.now(),
                    "https://google.com"
            );
            kafkaTemplate.send(KafkaTopics.URL_CLICKS, shortCode, event);
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = urlClickRepository.countByShortCode(shortCode);
            assertThat(count).isEqualTo(3);
        });

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                    .header(SecurityHeaders.X_USER_ID, 1L)
                    .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.topCountries.LOCAL").value(3))
                .andExpect(jsonPath("$.devices.DESKTOP").value(3))
                .andExpect(jsonPath("$.topBrowsers.Chrome").value(3));
    }

    @Test
    @DisplayName("Should be idempotent and ignore duplicate click events with the same eventId")
    void consume_DuplicateEventsWithSameEventId_IgnoresDuplicates() throws Exception {
        String shortCode = "idem123";
        UUID eventId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            UrlClickEvent event = new UrlClickEvent(
                    eventId,
                    shortCode,
                    "Mozilla/5.0",
                    "192.168.1.1",
                    Instant.now(),
                    "https://google.com"
            );
            kafkaTemplate.send(KafkaTopics.URL_CLICKS, shortCode, event);
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            long count = urlClickRepository.countByShortCode(shortCode);
            assertThat(count).isEqualTo(1);
        });

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode)
                    .header(SecurityHeaders.X_USER_ID, 1L)
                    .header(SecurityHeaders.X_USER_ROLE, SecurityHeaders.ROLE_ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").value(1));
    }
}

