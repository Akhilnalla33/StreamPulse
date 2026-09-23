package com.streampulse.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.streampulse.contracts.event.MonitoringEvent;
import com.streampulse.contracts.event.MonitoringEventType;
import com.streampulse.ingestion.dto.IngestEventRequest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class EventIngestionServiceTest {

    private KafkaTemplate<String, MonitoringEvent> kafkaTemplate;
    private EventIngestionService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(MonitoringEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        service = new EventIngestionService(kafkaTemplate);
    }

    @Test
    void publishesEventKeyedByTenantIdToEventsTopic() {
        IngestEventRequest request = new IngestEventRequest(
                "web-01", MonitoringEventType.CPU_USAGE, "cpu.usage.percent", 92.5, null, Map.of("region", "us-east-1"));

        String eventId = service.ingest("tenant-a", request);

        assertThat(eventId).isNotBlank();
        verify(kafkaTemplate).send(eq(EventIngestionService.EVENTS_TOPIC), eq("tenant-a"), any(MonitoringEvent.class));
    }

    @Test
    void generatesDistinctEventIdsPerCall() {
        IngestEventRequest request = new IngestEventRequest(
                "web-01", MonitoringEventType.ERROR_RATE, "error.rate", 5.0, null, Map.of());

        String first = service.ingest("tenant-a", request);
        String second = service.ingest("tenant-a", request);

        assertThat(first).isNotEqualTo(second);
    }
}
