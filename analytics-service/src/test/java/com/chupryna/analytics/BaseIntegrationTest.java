package com.chupryna.analytics;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            "postgres:16-alpine"
    );

    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:4.3.1")
    );

    static {
        postgres.start();
        kafka.start();
    }
}
