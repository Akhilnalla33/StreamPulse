package com.streampulse.contracts.event;

import java.time.Instant;
import java.util.Map;

/**
 * Canonical event published by {@code ingestion-service} to the {@code streampulse.events}
 * Kafka topic. Consumed by {@code rules-engine-service}. The topic is partitioned by
 * {@code tenantId} so all events for one tenant are strictly ordered.
 *
 * @param eventId     globally unique id assigned at ingestion time, used for consumer-side dedupe
 * @param tenantId    owning tenant
 * @param source      free-text origin of the event, e.g. host name or service name
 * @param type        kind of monitoring signal
 * @param metricName  name of the metric this event reports, e.g. {@code "cpu.usage.percent"}
 * @param value       numeric reading associated with the event
 * @param occurredAt  time the event occurred at the source (not ingestion time)
 * @param attributes  free-form key/value tags, e.g. region, host, deployment version
 */
public record MonitoringEvent(
        String eventId,
        String tenantId,
        String source,
        MonitoringEventType type,
        String metricName,
        double value,
        Instant occurredAt,
        Map<String, String> attributes) {

    public MonitoringEvent {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
