package com.streampulse.rules.dto;

import com.streampulse.contracts.event.AlertSeverity;
import com.streampulse.rules.entity.RuleComparator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleRequest(
        @NotBlank String name,
        @NotBlank String metricName,
        @NotNull RuleComparator comparator,
        @NotNull Double threshold,
        @NotNull AlertSeverity severity,
        boolean enabled) {
}
