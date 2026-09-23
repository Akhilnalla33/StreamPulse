package com.streampulse.ingestion.service;

import com.streampulse.contracts.event.MonitoringEvent;
import com.streampulse.ingestion.dto.IngestEventRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventIngestionService {

    public static final String EVENTS_TOPIC = "streampulse.events";

    private final KafkaTemplate<String, MonitoringEvent> kafkaTemplate;

    public EventIngestionService(KafkaTemplate<String, MonitoringEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @CircuitBreaker(name = "kafkaProducer")
    @Retry(name = "kafkaProducer")
    public String ingest(String tenantId, IngestEventRequest request) {
        String eventId = UUID.randomUUID().toString();
        MonitoringEvent event = new MonitoringEvent(
                eventId,
                tenantId,
                request.source(),
                request.type(),
                request.metricName(),
                request.value(),
                request.occurredAt() != null ? request.occurredAt() : Instant.now(),
                request.attributes() != null ? request.attributes() : Map.of());

        kafkaTemplate.send(EVENTS_TOPIC, tenantId, event);
        return eventId;
    }
}
