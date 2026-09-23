package com.streampulse.contracts.event;

/** The kind of monitoring signal a {@link MonitoringEvent} carries. */
public enum MonitoringEventType {
    CPU_USAGE,
    MEMORY_USAGE,
    ERROR_RATE,
    DEPLOYMENT,
    LATENCY
}
