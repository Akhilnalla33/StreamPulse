# ingestion-service

Stateless: validates incoming monitoring events and publishes them to the `streampulse.events`
Kafka topic, keyed by tenant. No database. See the root [README](../README.md) and
[`docs/CONTRACT.md`](../docs/CONTRACT.md).

Runs on `:8082`. OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`.
