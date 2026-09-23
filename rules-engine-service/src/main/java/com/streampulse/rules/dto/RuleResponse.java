package com.streampulse.rules.dto;

import com.streampulse.contracts.event.AlertSeverity;
import com.streampulse.rules.entity.AlertRule;
import com.streampulse.rules.entity.RuleComparator;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        String name,
        String metricName,
        RuleComparator comparator,
        double threshold,
        AlertSeverity severity,
        boolean enabled) {

    public static RuleResponse from(AlertRule rule) {
        return new RuleResponse(rule.getId(), rule.getName(), rule.getMetricName(), rule.getComparator(),
                rule.getThreshold(), rule.getSeverity(), rule.isEnabled());
    }
}
