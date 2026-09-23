# auth-service

Tenant registration, login, JWT access/refresh issuance (rotation-on-use), RBAC roles
(`ADMIN`/`MEMBER`/`VIEWER`), BCrypt password hashing. Owns `authdb`. See the root
[README](../README.md) and [`docs/CONTRACT.md`](../docs/CONTRACT.md) for the platform-wide
picture and the exact JWT claim layout `api-gateway` mirrors for local verification.

Runs on `:8081`. OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.
