package com.streampulse.notification.dto;

import com.streampulse.contracts.event.AlertEvent;
import com.streampulse.contracts.event.AlertSeverity;
import com.streampulse.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String alertId,
        String ruleId,
        String ruleName,
        AlertSeverity severity,
        String metricName,
        String message,
        String triggeringEventId,
        Instant firedAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getAlertId(), n.getRuleId(), n.getRuleName(), n.getSeverity(),
                n.getMetricName(), n.getMessage(), n.getTriggeringEventId(), n.getFiredAt());
    }

    public static NotificationResponse from(AlertEvent e) {
        return new NotificationResponse(null, e.alertId(), e.ruleId(), e.ruleName(), e.severity(),
                e.metricName(), e.message(), e.triggeringEventId(), e.firedAt());
    }
}
