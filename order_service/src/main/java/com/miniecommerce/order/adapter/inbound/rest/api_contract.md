# Order API Contract

Service: `order_service`
Base URL: `http://localhost:8083/api/orders`
Stack: Spring Web (servlet MVC) + Spring Data JPA + OpenFeign (calls `inventory_service`)

All responses are wrapped in the shared `ApiResponse<T>` (from the `common` module):

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "id": 1,
    "phoneId": 1,
    "quantity": 2,
    "status": "PENDING",
    "createdAt": "2026-08-28T13:49:51.829777400Z"
  },
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

## POST /api/orders

Place an order. The service first checks stock in `inventory_service` (via Feign,
`GET /api/phones/{phoneId}`) and only creates the order when stock is sufficient.

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "phoneId": 1,
    "quantity": 2
  }'
```

Request body (`OrderRequest`):

| Field    | Type    | Required | Note                                  |
|----------|---------|----------|---------------------------------------|
| phoneId  | long    | yes      | id of the phone in `inventory_service`|
| quantity | integer | yes      | must be > 0                           |

### Success — 200 OK

When `inventory_service` reports enough stock, the order is created with
`status = PENDING` and persisted (currently a stub adapter that assigns `id = 1`).

Response `200 OK` — `ApiResponse<Order>`:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "id": 1,
    "phoneId": 1,
    "quantity": 2,
    "status": "PENDING",
    "createdAt": "2026-08-28T13:49:51.829777400Z"
  },
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

### Out of stock — 409 Conflict

When `available < quantity`, the use case throws
`AppException(CONFLICT, "STOCK_NOT_ENOUGH", "Not enough stock for phone N")`.
The shared `GlobalExceptionHandler` (from `common`) returns HTTP 409:

```json
{
  "success": false,
  "code": "STOCK_NOT_ENOUGH",
  "message": "Not enough stock for phone 1",
  "data": null,
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

### Phone not found — 404 Not Found

If `inventory_service` answers `404 PHONE_NOT_FOUND`, the `CommonFeignErrorDecoder`
(from `common`) rebuilds the same `AppException` and it propagates to the order
service's `GlobalExceptionHandler`, returning HTTP 404 with `code: "PHONE_NOT_FOUND"`:

```json
{
  "success": false,
  "code": "PHONE_NOT_FOUND",
  "message": "Phone not found",
  "data": null,
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

## Notes

- This contract covers the flow **up to the Feign call to `inventory_service`**
  (check stock + create order as `PENDING`). Reserve/finalize/release, Kafka
  events, payment and expiry handling are not implemented yet.
- `inventory_service` must be reachable at `app.inventory.url`
  (default `http://localhost:8082`); override with `INVENTORY_URL`.
- Errors use the shared `AppException` from `common`; the `code` string
  (`STOCK_NOT_ENOUGH`, `PHONE_NOT_FOUND`, ...) is what callers should branch on,
  not the HTTP status.
- `id` and `createdAt` are server-generated; they must NOT be sent in the request.
- Architecture is hexagonal (mirrors `inventory_service`): `OrderController`
  (adapter/inbound) → `CreateOrderUseCase` (app/port/inbound) → `OrderService`
  (app/service) → `InventoryPort` (app/port/outbound, implemented by
  `InventoryFeignAdapter` over OpenFeign) + `SaveOrderPort` (stub).
