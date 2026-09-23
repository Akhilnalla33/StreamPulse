# StreamPulse cross-service contract (internal working doc)

Frozen conventions every service implementation must follow so the system integrates without
a second pass. Not part of the public README.

## Ports
- api-gateway: 8080
- auth-service: 8081
- ingestion-service: 8082
- rules-engine-service: 8083
- notification-service: 8084

## Databases (one Postgres instance in compose, one DB per service)
- auth-service -> `authdb`
- rules-engine-service -> `rulesdb`
- notification-service -> `notificationdb`
- ingestion-service: stateless, no database (validates + publishes to Kafka only)

## JWT (HS256, shared secret via env `JWT_SECRET`, min 256-bit)
- Issuer claim: `streampulse-auth`
- Claims: `sub` = user id (UUID string), `tenantId` (UUID string), `roles` (array of
  `ADMIN`|`MEMBER`|`VIEWER`), `type` = `access`|`refresh`
- Access token TTL: 15 minutes. Refresh token TTL: 7 days, rotated on every use (old one
  revoked server-side in auth-service's `refresh_tokens` table).
- api-gateway validates the access token's signature and expiry **locally** (no network call
  to auth-service) using the same `JWT_SECRET`, then forwards `X-Tenant-Id`, `X-User-Id`,
  `X-User-Roles` headers downstream. Downstream services trust those headers only when the
  request arrives from the gateway's internal network (compose network isolation); they also
  independently re-validate the JWT for defense in depth.

## Kafka topics (JSON payloads, see `event-contracts` module for the exact records)
- `streampulse.events` — key = `tenantId`, value = `MonitoringEvent`. Produced by
  ingestion-service, consumed by rules-engine-service (`consumer group: rules-engine`).
- `streampulse.alerts` — key = `tenantId`, value = `AlertEvent`. Produced by
  rules-engine-service, consumed by notification-service (`consumer group:
  notification-service`).
- Both topics: 3 partitions, replication factor 1 (single-broker dev compose).

## Redis (single instance, DB-per-purpose via key prefix)
- `ratelimit:{tenantId}:{route}` — gateway per-tenant token bucket (Bucket4j or manual Lua),
  100 requests/minute per tenant per route group, TTL 120s.
- `rule-cache:{tenantId}` — rules-engine caches each tenant's active `AlertRule` list, TTL 60s,
  evicted on rule create/update/delete.
- `idempotency:events:{eventId}` — rules-engine dedupe, TTL 24h.
- `idempotency:alerts:{alertId}` — notification-service dedupe, TTL 24h.

## Error response shape
Every service uses `com.streampulse.contracts.error.ErrorResponse` from `event-contracts` via
a `@RestControllerAdvice`. `traceId` = value of inbound `X-Correlation-Id` header, or a
generated UUID if absent; every service echoes it back on the response header too and includes
it in structured log lines (MDC key `traceId`).

## RBAC
- `ADMIN`: manage tenant users, manage alert rules, read everything.
- `MEMBER`: ingest events, read events/alerts/rules.
- `VIEWER`: read-only (events/alerts/rules).
Enforced with `@PreAuthorize` on controller methods, reading roles from the JWT
(`ROLE_ADMIN`, `ROLE_MEMBER`, `ROLE_VIEWER` authorities).
