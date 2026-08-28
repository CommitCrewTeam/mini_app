# Phone API Contract

Service: `inventory_service`
Base URL: `http://localhost:8082/api/phones`
Stack: Spring WebFlux + Spring Data R2DBC (reactive, `Flux`/`Mono` end-to-end)

All responses return the domain `Phone` object directly (no wrapper DTO):

```json
{
  "id": 1,
  "name": "iPhone 15",
  "detail": { "color": "black", "storage": "256GB", "ram": "8GB" },
  "active": true,
  "stock": 10
}
```

`detail` is a dynamic JSON object (`Map<String, Object>` in the domain), persisted as PostgreSQL `JSONB`.

## GET /api/phones

List all phones.

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8082/api/phones
```

Response `200 OK` — `Flux<Phone>`:

```json
[
  {
    "id": 1,
    "name": "iPhone 15",
    "detail": { "color": "black", "storage": "256GB", "ram": "8GB" },
    "active": true,
    "stock": 10
  }
]
```

## POST /api/phones

Create a phone.

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8082/api/phones \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15",
    "detail": {
      "color": "black",
      "storage": "256GB",
      "ram": "8GB"
    },
    "active": true,
    "stock": 10
  }'
```

Request body (`PhoneRequest`):

| Field   | Type                  | Required | Note                                  |
|---------|-----------------------|----------|---------------------------------------|
| name    | string                | yes      |                                       |
| detail  | object (JSON)         | yes      | arbitrary JSON, stored as `JSONB`     |
| active  | boolean               | yes      |                                       |
| stock   | integer               | yes      |                                       |

Response `200 OK` — `Mono<Phone>` with generated `id`:

```json
{
  "id": 1,
  "name": "iPhone 15",
  "detail": { "color": "black", "storage": "256GB", "ram": "8GB" },
  "active": true,
  "stock": 10
}
```

## Notes

- Prerequisites: PostgreSQL must be running and the `phones` table created by Liquibase
  (`detail JSONB NOT NULL DEFAULT '{}'`, `active BOOLEAN NOT NULL DEFAULT true`,
  `stock INTEGER NOT NULL DEFAULT 0`).
- `id` is server-generated (BIGSERIAL); it must NOT be sent in the POST body.
- The API is fully reactive; no JPA/Hibernate/`block()` is used.
