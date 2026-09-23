CREATE TABLE notifications (
    id                   UUID PRIMARY KEY,
    tenant_id            UUID NOT NULL,
    alert_id             VARCHAR(64) NOT NULL UNIQUE,
    rule_id              VARCHAR(64) NOT NULL,
    rule_name            VARCHAR(255) NOT NULL,
    severity             VARCHAR(20) NOT NULL,
    metric_name          VARCHAR(255) NOT NULL,
    message              TEXT NOT NULL,
    triggering_event_id  VARCHAR(64) NOT NULL,
    fired_at             TIMESTAMPTZ NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    version              BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notifications_tenant_fired_at ON notifications (tenant_id, fired_at DESC);
