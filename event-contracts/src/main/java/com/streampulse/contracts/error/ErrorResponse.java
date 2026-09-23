package com.streampulse.contracts.error;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error response shape returned by every StreamPulse service's
 * {@code @ControllerAdvice}. {@code traceId} is the correlation id propagated via the
 * {@code X-Correlation-Id} header (or generated if absent) so a failure can be traced across
 * services and log lines.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<String> details) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
