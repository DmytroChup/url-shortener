package com.chupryna.url_shortener.config;

import com.chupryna.url_shortener.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic urlClicksTopic() {
        return TopicBuilder
                .name(KafkaTopics.URL_CLICKS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
