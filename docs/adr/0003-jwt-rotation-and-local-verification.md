# ADR 0003: Short-lived JWTs with refresh rotation, verified locally at the edge

## Status
Accepted

## Context
Every request through `api-gateway` needs to know who's calling (user, tenant, roles) before
routing it, and every downstream service enforces RBAC on top of that identity. Two decisions
were needed: (1) how long-lived should tokens be and how do we limit the blast radius of a
stolen refresh token, and (2) does the gateway call `auth-service` over the network to validate
every single request, or verify the token itself.

## Decision
- Access tokens are HS256 JWTs, 15-minute TTL, carrying `sub` (user id), `tenantId`, and `roles`.
- Refresh tokens are separate JWTs, 7-day TTL, and are **rotated on every use**: `auth-service`
  stores only a SHA-256 hash of the current valid refresh token per session, marks it revoked
  the instant it's exchanged for a new pair, and rejects any later reuse of that same token
  outright (see `AuthService.refresh` and its integration test asserting a re-used refresh
  token gets `401`).
- `api-gateway` verifies the access token's signature and expiry **locally**, using the same
  `JWT_SECRET` shared with `auth-service` (HS256, symmetric) — it does not call `auth-service`
  over HTTP to validate a token on every request.

## Rationale
- **Short access-token TTL + rotation bounds theft impact.** If an access token leaks, it's
  useless after at most 15 minutes. If a refresh token leaks and gets used, the rotation-with
  revocation-on-reuse pattern means the legitimate holder's next refresh attempt will fail
  (their token was already invalidated), which is a detectable signal of compromise, and the
  attacker only gets one shot at a new pair before the trail goes cold.
- **Local verification avoids a hard runtime dependency.** If every gateway request had to call
  `auth-service` to check a token, `auth-service` becomes a single point of failure for the
  entire platform's request path, and adds a network hop of latency to every single call. HS256
  with a shared secret gives the gateway everything it needs to verify authenticity and expiry
  without a round trip — this is explicitly the trade-off called out in the assignment's own
  resilience requirements ("prefer local JWT signature verification over a network call where
  possible").
- **HS256 (symmetric) over RS256 (asymmetric) was chosen for simplicity** given `auth-service`
  is the only issuer and `api-gateway` is the only verifier at the edge (downstream services
  trust the gateway's forwarded identity headers rather than re-verifying independently at the
  JWT level, per `docs/CONTRACT.md`). A public/private keypair (RS256) would be the natural next
  step if a third party ever needed to verify StreamPulse tokens without holding the signing
  secret.

## Consequences
- The signing secret (`JWT_SECRET`) is now a piece of shared configuration between two services
  instead of being fully private to `auth-service`. It must be distributed the same way to both
  (`.env` / secret manager in production), and rotating it invalidates every outstanding token
  platform-wide — there's no key-ID/rollover mechanism in this version.
- Revoking a specific *access* token before its 15-minute TTL expires is not possible (no
  server-side access-token blacklist) — this is an accepted trade for avoiding a lookup on every
  request. Revoking a session works by revoking its refresh token, which prevents the session
  from renewing.
