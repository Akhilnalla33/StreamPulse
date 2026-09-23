package com.streampulse.contracts.event;

import java.time.Instant;

/**
 * Published by {@code rules-engine-service} to the {@code streampulse.alerts} Kafka topic
 * whenever an incoming {@link MonitoringEvent} matches a tenant alert rule. Consumed by
 * {@code notification-service}, which delivers it to connected clients and persists it.
 *
 * @param alertId          globally unique id for this alert firing, used for consumer-side dedupe
 * @param tenantId         owning tenant
 * @param ruleId           id of the {@code AlertRule} that fired
 * @param ruleName         human-readable name of the rule, snapshotted at fire time
 * @param severity         severity configured on the rule
 * @param metricName       metric that triggered the rule
 * @param thresholdValue   threshold configured on the rule
 * @param actualValue      actual value that breached the threshold
 * @param triggeringEventId id of the {@link MonitoringEvent} that caused this alert
 * @param message          human-readable alert message
 * @param firedAt          time the rule evaluation fired
 */
public record AlertEvent(
        String alertId,
        String tenantId,
        String ruleId,
        String ruleName,
        AlertSeverity severity,
        String metricName,
        double thresholdValue,
        double actualValue,
        String triggeringEventId,
        String message,
        Instant firedAt) {
}
