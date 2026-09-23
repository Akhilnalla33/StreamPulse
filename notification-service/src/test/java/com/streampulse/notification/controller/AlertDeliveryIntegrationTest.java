package com.streampulse.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.contracts.event.AlertEvent;
import com.streampulse.contracts.event.AlertSeverity;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * End-to-end proof of alert -> WebSocket delivery under a time budget: publishes an
 * {@link AlertEvent} directly to {@code streampulse.alerts} (standing in for a real
 * ingest -> rule-match pipeline exercised individually by ingestion-service's and
 * rules-engine-service's own integration tests) and asserts a connected client receives it
 * within 5 seconds. Requires Docker; see repo README for the honest status of this run.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AlertDeliveryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notificationdb")
            .withUsername("streampulse")
            .withPassword("streampulse");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void alertPublishedToKafkaIsDeliveredOverWebSocketWithinTimeBudget() throws Exception {
        String tenantId = UUID.randomUUID().toString();
        AtomicReference<String> received = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("X-Tenant-Id", tenantId);
        headers.add("X-User-Id", "user-1");
        headers.add("X-User-Roles", "MEMBER");

        WebSocketSession session = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                received.set(message.getPayload());
                latch.countDown();
            }
        }, headers, java.net.URI.create("ws://localhost:" + port + "/ws/notifications")).get(5, TimeUnit.SECONDS);

        AlertEvent alert = new AlertEvent(
                UUID.randomUUID().toString(), tenantId, "rule-1", "High CPU", AlertSeverity.CRITICAL,
                "cpu.usage.percent", 90.0, 97.5, "evt-1", "CPU too high", Instant.now());
        publishToKafka(alert);

        boolean deliveredInTime = latch.await(5, TimeUnit.SECONDS);
        session.close();

        assertThat(deliveredInTime).isTrue();
        assertThat(received.get()).isNotNull();
        assertThat(objectMapper.readTree(received.get()).get("alertId").asText()).isEqualTo(alert.alertId());
    }

    private void publishToKafka(AlertEvent alert) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        try (KafkaProducer<String, AlertEvent> producer = new KafkaProducer<>(config)) {
            producer.send(new ProducerRecord<>("streampulse.alerts", alert.tenantId(), alert));
        }
    }
}
