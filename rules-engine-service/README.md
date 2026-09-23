# rules-engine-service

Stores per-tenant alert rules (`rulesdb`, Postgres), caches the active rule set per tenant in
Redis (`rule-cache:{tenantId}`, 60s TTL), consumes `streampulse.events` and evaluates each event
against the owning tenant's rules, publishing `streampulse.alerts` for every breach. Dedupes
re-delivered events via Redis (`idempotency:events:{eventId}`, 24h TTL). See the root
[README](../README.md) and [`docs/CONTRACT.md`](../docs/CONTRACT.md).

Runs on `:8083`. OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.
