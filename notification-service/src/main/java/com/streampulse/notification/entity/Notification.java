package com.streampulse.notification.entity;

import com.streampulse.contracts.event.AlertEvent;
import com.streampulse.contracts.event.AlertSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "alert_id", nullable = false, unique = true)
    private String alertId;

    @Column(name = "rule_id", nullable = false)
    private String ruleId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(nullable = false)
    private String message;

    @Column(name = "triggering_event_id", nullable = false)
    private String triggeringEventId;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected Notification() {
    }

    public static Notification fromAlertEvent(AlertEvent event) {
        Notification notification = new Notification();
        notification.id = UUID.randomUUID();
        notification.tenantId = UUID.fromString(event.tenantId());
        notification.alertId = event.alertId();
        notification.ruleId = event.ruleId();
        notification.ruleName = event.ruleName();
        notification.severity = event.severity();
        notification.metricName = event.metricName();
        notification.message = event.message();
        notification.triggeringEventId = event.triggeringEventId();
        notification.firedAt = event.firedAt();
        notification.createdAt = Instant.now();
        return notification;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getAlertId() {
        return alertId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getMessage() {
        return message;
    }

    public String getTriggeringEventId() {
        return triggeringEventId;
    }

    public Instant getFiredAt() {
        return firedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
