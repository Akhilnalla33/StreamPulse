package com.streampulse.ingestion.dto;

import com.streampulse.contracts.event.MonitoringEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record IngestEventRequest(
        @NotBlank String source,
        @NotNull MonitoringEventType type,
        @NotBlank String metricName,
        @NotNull Double value,
        Instant occurredAt,
        Map<String, String> attributes) {
}
