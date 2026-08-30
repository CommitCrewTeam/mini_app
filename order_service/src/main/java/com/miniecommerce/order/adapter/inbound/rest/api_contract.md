# Order API Contract

Service: `order_service`
Base URL: `http://localhost:8083/api/orders`
Stack: Spring Web (servlet MVC) + OpenFeign + Spring Kafka

All responses are wrapped in the shared `ApiResponse<T>` (from the `common` module):

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "id": "6f2a8e9d-1e3a-4b2c-9d8f-0a1b2c3d4e5f",
    "customerId": "c-1",
    "items": [
      {
        "productId": "p-1",
        "quantity": 2,
        "unitPrice": 15000000
      }
    ],
    "shippingFee": 20000,
    "totalAmount": 30020000,
    "status": "PENDING",
    "createdAt": "2026-08-28T13:49:51.829777400Z",
    "updatedAt": "2026-08-28T13:49:51.829777400Z"
  },
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

## POST /api/orders

Place an order. `OrderRestMapper` translates the request into a plain
`CreateOrderCommand` (no business logic); `PlaceOrderApplicationService` builds the
`OrderAggregateRoot` (via `Order.create(customerId, shippingFee)` then
`addItem(...)` for each line), validates it, persists it, and publishes the event.

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "c-1",
    "shippingFee": 20000,
    "items": [
      { "productId": "p-1", "quantity": 2, "unitPrice": 15000000 }
    ]
  }'
```

Request body (`OrderRequest`):

| Field       | Type    | Required | Note                                       |
|-------------|---------|----------|--------------------------------------------|
| customerId  | string  | yes      | must not be blank                          |
| shippingFee | long    | yes      | money in VND, must be >= 0                 |
| items       | array   | yes      | at least 1 item required (see below)       |
| items[].productId | string | yes  | must not be blank                          |
| items[].quantity  | integer | yes | must be > 0                                |
| items[].unitPrice | long    | yes | money in VND, must be >= 0                 |

### Success — 200 OK

The order is created as `status = PENDING` (UUID `id`, timestamps server-generated),
persisted by `OrderPersistenceAdapter` (JPA over `OrderEntity`/`OrderItemEntity`,
schema managed by Liquibase), then an `OrderPlacedEvent` is mapped from the saved
aggregate (`toOrderPlacedEvent`, inside `PlaceOrderApplicationService`) and published
to Kafka topic `order.placed` (fire-and-forget). The event payload is a dedicated
event object, never the domain aggregate.

`totalAmount` is computed by the aggregate: `SUM(quantity * unitPrice) + shippingFee`.

Response `200 OK` — `ApiResponse<OrderResponse>`:

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "id": "6f2a8e9d-1e3a-4b2c-9d8f-0a1b2c3d4e5f",
    "customerId": "c-1",
    "items": [
      {
        "productId": "p-1",
        "quantity": 2,
        "unitPrice": 15000000
      }
    ],
    "shippingFee": 20000,
    "totalAmount": 30020000,
    "status": "PENDING",
    "createdAt": "2026-08-28T13:49:51.829777400Z",
    "updatedAt": "2026-08-28T13:49:51.829777400Z"
  },
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

### Invalid domain data — 400 Bad Request

Domain invariants (thrown as `AppException(ErrorCode.BAD_REQUEST, message)`, handled
by the shared `GlobalExceptionHandler`): empty items, blank `customerId`,
`shippingFee < 0`, blank `productId`, `quantity <= 0`, `unitPrice < 0`.

```json
{
  "success": false,
  "code": "BAD_REQUEST",
  "message": "Order must have at least one item to be placed",
  "data": null,
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

## Domain rules (OrderAggregateRoot)

- State machine: `PENDING --confirm()--> CONFIRMED`,
  `PENDING --cancel()--> CANCELLED`, `CONFIRMED --cancel()--> CANCELLED`.
- Invalid state transitions throw `AppException` with `ErrorCode.CONFLICT`
  (e.g. modifying items after `PENDING`, confirming without items, cancelling an
  already-cancelled order).
- `totalAmount()` is always computed; there is no setter.

## Notes

- Kafka topic: `app.kafka.topics.order-placed` (default `order.placed`).
- Inventory stock verification and Customer communication are **out of scope**
  for this iteration; the `InventoryPort`/`InventoryFeignAdapter`/`InventoryClient`
  files remain for the next step but are not wired into the place-order flow.
- Domain Event and Outbox are not implemented yet; persistence **is** implemented
  via JPA (`OrderPersistenceAdapter` + `OrderEntity`/`OrderItemEntity`) with the
  schema managed by Liquibase (`db/changelog`).
- Errors use the shared `AppException(ErrorCode, message)` from `common`. The
  `code` field in the response is just the category (`BAD_REQUEST`, `NOT_FOUND`,
  `CONFLICT`, `INTERNAL_ERROR`, ...) and maps 1:1 to the HTTP status via
  `ErrorCode`; the domain never knows about HTTP or Spring. The `message` carries
  the specific detail callers care about.
- Responses expose **`OrderResponse` (REST DTO) only**; the domain
  `OrderAggregateRoot`/`OrderItem`/`MoneyValue` are never serialized to clients.
  Money fields are flattened to `long` (VND).
- `id`, `createdAt`, `updatedAt`, `totalAmount` are server-generated; they must
  NOT be sent in the request.
- Architecture is hexagonal (mirrors `inventory_service`): `OrderController`
  (adapter/inbound) → `OrderRestMapper` (request → `CreateOrderCommand`) →
  `CreateOrderUseCase` (app/port/inbound) → `PlaceOrderApplicationService`
  (app/service, builds the aggregate + orchestrates) → `PlaceOrderDomainService`
  (domain) + `SaveOrderPort`/`LoadOrderPort` (app/port/outbound, implemented by
  `OrderPersistenceAdapter`) + `PublishOrderEventPort` (app/port/outbound,
  `OrderKafkaProducer`).
- Domain naming follows DDD role suffixes: `OrderAggregateRoot` (aggregate),
  `OrderItem` (domain entity), `MoneyValue` (value object); the JPA row mappings
  use a separate `OrderEntity`/`OrderItemEntity` in the persistence adapter.