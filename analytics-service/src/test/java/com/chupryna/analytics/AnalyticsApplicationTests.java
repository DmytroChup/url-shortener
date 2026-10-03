package com.chupryna.analytics;

import com.chupryna.analytics.repository.UrlClickRepository;
import com.chupryna.url_shortener.event.KafkaTopics;
import com.chupryna.url_shortener.event.UrlClickEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
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

    @Test
    @DisplayName("Should consume click events from Kafka, persist to DB, and return aggregated metrics via REST")
    void shouldConsumeClickEventsAndExposeMetrics() throws Exception {
        String shortCode = "abc1234";

        for (int i = 0; i < 3; i++) {
            UrlClickEvent event = new UrlClickEvent(
                    UUID.randomUUID(),
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
            assertThat(count).isEqualTo(3);
        });

        mockMvc.perform(get("/api/v1/analytics/{shortCode}", shortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value(shortCode))
                .andExpect(jsonPath("$.totalClicks").value(3));
    }
}

