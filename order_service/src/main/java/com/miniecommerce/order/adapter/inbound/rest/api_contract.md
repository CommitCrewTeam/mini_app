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
        "unitPrice": { "amount": 15000000 }
      }
    ],
    "shippingFee": { "amount": 20000 },
    "totalAmount": { "amount": 30020000 },
    "status": "PENDING",
    "createdAt": "2026-08-28T13:49:51.829777400Z",
    "updatedAt": "2026-08-28T13:49:51.829777400Z"
  },
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

## POST /api/orders

Place an order. The request builds the `OrderAggregateRoot` (domain aggregate) via
`OrderRestMapper`; `Order.create(customerId, shippingFee)` then `addItem(...)` for each line.

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
schema managed by Liquibase), then an OrderCreated event is published to Kafka topic
`order.placed` (fire-and-forget).

`totalAmount` is computed by the aggregate: `SUM(quantity * unitPrice) + shippingFee`.

Response `200 OK` — `ApiResponse<OrderAggregateRoot>`:

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
        "unitPrice": { "amount": 15000000 }
      }
    ],
    "shippingFee": { "amount": 20000 },
    "totalAmount": { "amount": 30020000 },
    "status": "PENDING",
    "createdAt": "2026-08-28T13:49:51.829777400Z",
    "updatedAt": "2026-08-28T13:49:51.829777400Z"
  },
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

### Invalid domain data — 400 Bad Request

Domain invariants (thrown as `AppException`, handled by the shared
`GlobalExceptionHandler`):
- empty items → `ORDER_HAS_NO_ITEMS`
- blank `customerId` → `INVALID_CUSTOMER`
- `shippingFee < 0` → `INVALID_MONEY`
- blank `productId` → `INVALID_ITEM_PRODUCT`
- `quantity <= 0` → `INVALID_ITEM_QUANTITY`
- `unitPrice < 0` → `INVALID_ITEM_UNIT_PRICE`

```json
{
  "success": false,
  "code": "ORDER_HAS_NO_ITEMS",
  "message": "Order must have at least one item to be placed",
  "data": null,
  "timestamp": "2026-08-28T13:49:51.829777400Z"
}
```

## Domain rules (OrderAggregateRoot)

- State machine: `PENDING --confirm()--> CONFIRMED`,
  `PENDING --cancel()--> CANCELLED`, `CONFIRMED --cancel()--> CANCELLED`.
- Items (OrderItemEntity) can only be added/removed/updated while `status = PENDING`;
  otherwise `ORDER_NOT_MODIFIABLE`.
- `confirm()` requires at least one item (`ORDER_HAS_NO_ITEMS`) and status `PENDING`
  (`ORDER_NOT_CONFIRMABLE`); `cancel()` on an already cancelled order →
  `ORDER_NOT_CANCELLABLE`.
- `totalAmount()` is always computed; there is no setter.

## Notes

- Kafka topic: `app.kafka.topics.order-placed` (default `order.placed`).
- Inventory stock verification and Customer communication are **out of scope**
  for this iteration; the `InventoryPort`/`InventoryFeignAdapter`/`InventoryClient`
  files remain for the next step but are not wired into the place-order flow.
- Domain Event and Outbox are not implemented yet; persistence **is** implemented
  via JPA (`OrderPersistenceAdapter` + `OrderEntity`/`OrderItemEntity`) with the
  schema managed by Liquibase (`db/changelog`).
- Errors use the shared `AppException` from `common`; the `code` string
  (`ORDER_HAS_NO_ITEMS`, `INVALID_ITEM_QUANTITY`, ...) is what callers should
  branch on, not the HTTP status.
- `id`, `createdAt`, `updatedAt`, `totalAmount` are server-generated; they must
  NOT be sent in the request.
- Architecture is hexagonal (mirrors `inventory_service`): `OrderController`
  (adapter/inbound) → `CreateOrderUseCase` (app/port/inbound) →
  `PlaceOrderApplicationService` (app/service) → `PlaceOrderDomainService`
  (domain) + `SaveOrderPort`/`LoadOrderPort` (app/port/outbound, implemented by
  `OrderPersistenceAdapter`) + `PublishOrderEventPort` (app/port/outbound,
  `OrderKafkaProducer`).
- Domain naming follows DDD role suffixes: `OrderAggregateRoot` (aggregate),
  `OrderItem` (domain entity), `MoneyValue` (value object); the JPA row mappings
  use a separate `OrderEntity`/`OrderItemEntity` in the persistence adapter.