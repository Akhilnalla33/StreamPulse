# notification-service

Consumes `streampulse.alerts`, persists notification history (`notificationdb`, Postgres),
and pushes each alert to every connected client of the owning tenant over plain WebSocket
(`/ws/notifications`) and an SSE fallback (`/sse/notifications`), strictly scoped per tenant.
Dedupes re-delivered alerts via Redis (`idempotency:alerts:{alertId}`, 24h TTL). See the root
[README](../README.md) and [`docs/CONTRACT.md`](../docs/CONTRACT.md).

Runs on `:8084`. OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.
