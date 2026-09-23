package com.streampulse.ingestion.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.streampulse.contracts.event.MonitoringEvent;
import com.streampulse.ingestion.dto.IngestEventRequest;
import com.streampulse.ingestion.dto.IngestEventResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves ingestion-service actually publishes a well-formed {@link MonitoringEvent} to the
 * real {@code streampulse.events} Kafka topic via a Testcontainers broker. Requires Docker;
 * see repo README for the honest status of whether this ran in the build environment.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventIngestionIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private Consumer<String, MonitoringEvent> consumer;

    @BeforeEach
    void setUpConsumer() {
        Map<String, Object> props = new HashMap<>(KafkaTestUtils.consumerProps("test-group", "true", kafka.getBootstrapServers()));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MonitoringEvent.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
        consumer.subscribe(java.util.List.of("streampulse.events"));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void ingestedEventIsPublishedToKafkaWithinTimeBudget() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-Id", "tenant-integration-test");
        headers.add("X-User-Id", "user-1");
        headers.add("X-User-Roles", "MEMBER");
        IngestEventRequest request = new IngestEventRequest(
                "web-01", com.streampulse.contracts.event.MonitoringEventType.CPU_USAGE, "cpu.usage.percent", 97.2, null, Map.of());

        ResponseEntity<IngestEventResponse> response = restTemplate.postForEntity(
                url("/api/v1/events"), new HttpEntity<>(request, headers), IngestEventResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String eventId = response.getBody().eventId();

        ConsumerRecord<String, MonitoringEvent> record = KafkaTestUtils.getSingleRecord(consumer, "streampulse.events", Duration.ofSeconds(10));
        assertThat(record.key()).isEqualTo("tenant-integration-test");
        assertThat(record.value().eventId()).isEqualTo(eventId);
        assertThat(record.value().tenantId()).isEqualTo("tenant-integration-test");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
