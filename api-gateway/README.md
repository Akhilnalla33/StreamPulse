# api-gateway

Spring Cloud Gateway (WebFlux) edge. Verifies access-token JWTs locally (no network call to
auth-service), forwards identity as `X-Tenant-Id`/`X-User-Id`/`X-User-Roles` headers, applies
Redis-backed per-tenant rate limiting, and wraps every downstream route in a Resilience4j
circuit breaker with a JSON fallback response. See the root [README](../README.md) and
[`docs/CONTRACT.md`](../docs/CONTRACT.md).

Runs on `:8080` — this is the only service meant to be reachable from outside the compose
network.
