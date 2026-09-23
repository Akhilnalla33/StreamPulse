# ADR 0001: Kafka over RabbitMQ for inter-service events

## Status
Accepted

## Context
`ingestion-service` needs to hand off every incoming monitoring event to `rules-engine-service`,
and `rules-engine-service` needs to hand off every fired alert to `notification-service`. Both
hops are async, high-volume, and the events need to be replayable: if `rules-engine-service` is
down or being redeployed, events must queue up rather than be dropped, and a new consumer
(e.g. a future analytics service) should be able to read the same event stream from an earlier
offset without ingestion-service knowing or caring.

## Decision
Use Apache Kafka for both hops (`streampulse.events`, `streampulse.alerts`), not RabbitMQ or
another broker.

## Rationale
- **Log-based replay.** Kafka retains messages for a configurable window regardless of whether
  they've been consumed, so a new consumer group (analytics, a debugging tool, a backfill job)
  can replay history. RabbitMQ's queues are consumed-and-gone by default; replay requires extra
  machinery (shovel, dead-lettering into a new queue).
- **Partitioning by tenant.** Both topics are keyed by `tenantId`, which guarantees per-tenant
  ordering (a tenant's events are processed in the order they were ingested) while still
  parallelizing across tenants via partitions. This maps naturally onto Kafka's partition model;
  RabbitMQ would need per-tenant queues or consistent-hash exchanges to get the same property.
- **Consumer groups give free horizontal scaling.** Running multiple instances of
  `rules-engine-service` just means adding consumers to the same group — Kafka rebalances
  partitions automatically. This is the deployment topology the service is built for from day
  one.
- **This is a monitoring/alerting platform** — the domain is inherently stream-shaped (a
  continuous flow of metric events), which is Kafka's home turf, not RabbitMQ's (which shines
  more at task/command routing with complex routing topologies).

## Consequences
- Operational cost: running Kafka (even single-broker KRaft mode in dev compose) is heavier
  than RabbitMQ for local development. Accepted as a fair trade for the properties above.
- Consumers must handle at-least-once delivery themselves. This is why both
  `rules-engine-service` and `notification-service` do Redis-backed idempotency dedupe on
  `eventId`/`alertId` rather than relying on exactly-once delivery from the broker.
