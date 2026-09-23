# ADR 0002: One database per service, no shared schema access

## Status
Accepted

## Context
`auth-service`, `rules-engine-service`, and `notification-service` each own persistent state
(users/tenants, alert rules, notification history respectively). The obvious shortcut is one
shared Postgres database with one schema per service, or worse, all three sharing tables — e.g.
`notification-service` joining directly against `rules-engine-service`'s `alert_rules` table to
avoid duplicating `ruleName` on each notification.

## Decision
Each service gets its own logical database (`authdb`, `rulesdb`, `notificationdb`) inside the
single dev-compose Postgres instance, with its own JDBC credentials scoped to only that
database, and Flyway migrations that only that service ever runs. No service's JPA
`EntityManager` is ever pointed at another service's database.

## Rationale
- **Ownership boundary matches deploy boundary.** If `rules-engine-service` changes its
  `alert_rules` schema (e.g. splits `threshold` into `warning_threshold`/`critical_threshold`),
  that's a one-service migration and deploy. If `notification-service` had a foreign key into
  that table, the two services would have to deploy in lockstep, which defeats the point of
  having separate services.
- **Failure isolation.** A slow query or lock contention in `notificationdb` (a
  history table that grows unbounded) cannot degrade `rulesdb` (a small, hot, latency-sensitive
  table read on every event). Sharing a database means sharing that blast radius.
- **The "duplicate ruleName" cost is small and worth it.** `AlertEvent` (the Kafka message)
  carries `ruleName` as a denormalized snapshot at fire time, which `notification-service`
  persists directly. This is intentional: it means a notification's history is self-contained
  and immutable even if the rule is later renamed or deleted — arguably more correct than a
  live join would be, not just an isolation workaround.

## Consequences
- No cross-service SQL joins, ever. Any view that needs data from two services' domains (e.g.
  "notifications with the current rule name") has to either denormalize at write time (as above)
  or query both services and join in application code / at the API layer.
- Local dev runs all three databases in one Postgres container for simplicity (one process to
  start), but they are still logically separate databases with separate credentials — this is a
  compose convenience, not a violation of the boundary. Each service could point at a wholly
  separate Postgres instance in production with no code change, only a connection string.
