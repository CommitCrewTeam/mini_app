# Phone API Contract

Service: `inventory_service`
Base URL: `http://localhost:8082/api/phones`
Stack: Spring WebFlux + Spring Data R2DBC (reactive, `Flux`/`Mono` end-to-end)

All responses are wrapped in the shared `ApiResponse<T>` (from the `common` module):

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": 1,
    "name": "iPhone 15",
    "detail": { "color": "black", "storage": "256GB", "ram": "8GB" },
    "active": true,
    "stock": 10
  },
  "timestamp": 1724851200000
}
```

`detail` is a dynamic JSON object (`Map<String, Object>` in the domain), persisted as PostgreSQL `JSONB`.

## GET /api/phones

List all phones.

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8082/api/phones
```

Response `200 OK` — `Mono<ApiResponse<List<Phone>>>`:

```json
{
  "success": true,
  "message": null,
  "data": [
    {
      "id": 1,
      "name": "iPhone 15",
      "detail": { "color": "black", "storage": "256GB", "ram": "8GB" },
      "active": true,
      "stock": 10
    }
  ],
  "timestamp": 1724851200000
}
```

## GET /api/phones/{id}

Check the stock of a phone by id (returns only the `stock` field).

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8082/api/phones/1
```

The stock is fetched via a raw SQL query
(`SELECT stock FROM phones WHERE id = :id`) in `PhoneR2dbcRepository`.

Response `200 OK` — `Mono<ApiResponse<Integer>>` (data is the stock quantity):

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": 10,
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

If the id does not exist, the controller throws
`AppException(ErrorCode.NOT_FOUND, "Phone not found")`; the shared
`GlobalExceptionHandler` (from `common`) maps it to HTTP 404 and returns body
`code: "NOT_FOUND"` with `data: null`.

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "Phone not found",
  "data": null,
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
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

Response `200 OK` — `Mono<ApiResponse<Phone>>` with generated `id`:

```json
{
  "success": true,
  "message": "Phone created",
  "data": {
    "id": 1,
    "name": "iPhone 15",
    "detail": { "color": "black", "storage": "256GB", "ram": "8GB" },
    "active": true,
    "stock": 10
  },
  "timestamp": 1724851200000
}
```

## Notes

- Prerequisites: PostgreSQL must be running and the `phones` table created by Liquibase
  (`detail JSONB NOT NULL DEFAULT '{}'`, `active BOOLEAN NOT NULL DEFAULT true`,
  `stock INTEGER NOT NULL DEFAULT 0`).
- `id` is server-generated (BIGSERIAL); it must NOT be sent in the POST body.
- The API is fully reactive; no JPA/Hibernate/`block()` is used.
