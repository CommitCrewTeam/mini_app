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

## POST /api/orders/preview

Preview an order before placing it. Returns, per line item, the enriched catalog
data (name/detail/active/stock) fetched live from `inventory_service` via the
batch endpoint `/api/phones/by-ids`, plus active shipping/payment options from
`shipping_service` and `payment_service`. `PreviewOrderApplicationService`
orchestrates the three downstream calls in parallel (CompletableFuture) and joins.

Request body (`OrderRequest` — same shape as `POST /api/orders`; `shippingFee`
is accepted but **ignored** for preview):

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8083/api/orders/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "c-1",
    "shippingFee": 0,
    "items": [
      { "productId": "1", "quantity": 2, "unitPrice": 30000000 },
      { "productId": "2", "quantity": 1, "unitPrice": 25000000 }
    ]
  }'
```

| Field             | Type    | Required | Note                                                   |
|-------------------|---------|----------|--------------------------------------------------------|
| customerId        | string  | yes      | must not be blank                                      |
| shippingFee       | long    | ignored  | parsed by `OrderRequest`, not used in preview          |
| items             | array   | yes      | at least 1 item required                               |
| items[].productId | string  | yes      | must parse to a phone id (`Long`) to resolve inventory |
| items[].quantity  | integer | yes      | must be > 0                                            |
| items[].unitPrice | long    | yes      | money in VND, must be >= 0                             |

### Success — 200 OK

`detail` is the `JSONB` column of the `phones` row as a raw `Map`, `available =
active && stock >= quantity`. `subtotal = SUM(quantity * unitPrice)`, and
`totalAmount = subtotal` (no shipping fee applied in preview).

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "customerId": "c-1",
    "items": [
      {
        "productId": "1",
        "quantity": 2,
        "unitPrice": 30000000,
        "name": "iPhone 15 Pro Max",
        "detail": { "brand": "Apple", "color": "Natural Titanium", "storage": "256GB" },
        "active": true,
        "stock": 25,
        "available": true
      },
      {
        "productId": "2",
        "quantity": 1,
        "unitPrice": 25000000,
        "name": "Samsung Galaxy S24 Ultra",
        "detail": { "brand": "Samsung", "color": "Titanium Gray", "storage": "512GB" },
        "active": true,
        "stock": 18,
        "available": true
      }
    ],
    "shippingOptions": [
      { "code": "STANDARD", "name": "Standard Delivery", "baseFee": 15000 },
      { "code": "EXPRESS", "name": "Express Delivery", "baseFee": 30000 },
      { "code": "SAME_DAY", "name": "Same Day Delivery", "baseFee": 45000 }
    ],
    "paymentOptions": [
      { "code": "COD", "name": "Cash on Delivery" },
      { "code": "VNPAY", "name": "VNPay" },
      { "code": "MOMO", "name": "Momo E-Wallet" },
      { "code": "VISA", "name": "Credit / Debit Card" }
    ],
    "subtotal": 85000000,
    "totalAmount": 85000000
  },
  "timestamp": "2026-09-02T13:58:57.508+07:00"
}
```

### Missing / inactive inventory — available=false

If a `productId` does not resolve (unknown id or not parseable), the item
responses with `null` name/detail, `active=false`, `stock=0`, `available=false`
(`InventoryFeignAdapter` fills defaults); it is still included in the items list.

```json
{
  "success": true,
  "code": "200",
  "message": null,
  "data": {
    "customerId": "c-1",
    "items": [
      {
        "productId": "XXX",
        "quantity": 1,
        "unitPrice": 1000000,
        "name": null,
        "detail": null,
        "active": false,
        "stock": 0,
        "available": false
      }
    ],
    "shippingOptions": [],
    "paymentOptions": [],
    "subtotal": 1000000,
    "totalAmount": 1000000
  },
  "timestamp": "2026-09-02T13:58:57.508+07:00"
}
```

### Invalid domain data — 400 Bad Request

Empty `items` triggers the same domain check as place-order (`AppException` /
`BAD_REQUEST`). Shipping/payment options are only populated when the
downstream services are reachable and return active rows.

## Domain rules (OrderAggregateRoot)

- State machine: `PENDING --confirm()--> CONFIRMED`,
  `PENDING --cancel()--> CANCELLED`, `CONFIRMED --cancel()--> CANCELLED`.
- Invalid state transitions throw `AppException` with `ErrorCode.CONFLICT`
  (e.g. modifying items after `PENDING`, confirming without items, cancelling an
  already-cancelled order).
- `totalAmount()` is always computed; there is no setter.

## Notes

- Kafka topic: `app.kafka.topics.order-placed` (default `order.placed`).
- **Preview** (`POST /api/orders/preview`) reads live data from
  `inventory_service` (`GET /api/phones/by-ids`), `shipping_service`
  (`GET /api/v1/shipping-methods`) and `payment_service`
  (`GET /api/v1/payment-methods`) via Feign clients, run in parallel.
  The place-order flow itself does **not** yet verify stock.
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