# Mini Ecommerce Microservices - Master Plan

> Goal: Build a production-inspired microservice system using Spring Boot,
> Kafka, Redis and PostgreSQL.

## Current Scope

- Focus on the core ecommerce flow: product browsing, inventory reservation,
  order lifecycle, fake payment, notification, analytics and audit.
- Use a Gradle mono repo: one repository contains all service projects and
  shared modules.
- Service schemas are owned by their own service. Seed data is handled by a
  dedicated Seed Service.
- No Spring Cloud Gateway for now.
- No Spring Security, JWT or authentication/authorization feature for now.
- Client calls target services directly in local/dev flow.
- Inventory Service uses Spring WebFlux as the focused reactive learning target
  because flash-sale stock reservation has high concurrent traffic.

## Tech Stack

- Java 21
- Spring Boot 3
- Gradle multi-project build
- Spring WebMVC for most services
- Spring WebFlux for Inventory Service
- Spring Data JPA for blocking services
- Spring Data R2DBC for WebFlux services when database access is needed
- Liquibase for database migration in every service
- Dedicated Seed Service using Liquibase raw SQL seed scripts
- Spring Kafka
- Redis
- PostgreSQL
- Docker Compose
- OpenAPI

---

# Service Runtime Choice

| Service | Runtime | Reason |
| --- | --- | --- |
| Product | Spring Boot WebMVC | Product CRUD, product detail/list/search, Redis cache; keep simple with JPA |
| Inventory | Spring WebFlux | High concurrent stock reservation during flash sale; main reactive learning target |
| Order | Spring Boot WebMVC | Order transaction/state lifecycle is easier and clearer with blocking JPA first |
| Payment | Spring Boot WebMVC | Fake payment flow, not the main bottleneck for now |
| Notification | Spring Boot WebMVC | Async email simulation via Kafka |
| Analytics | Spring Boot WebMVC | Async event consumer for business metrics/read models |
| Audit | Spring Boot WebMVC | Async event persistence for trace/debug/history |
| Seed | Spring Boot non-web runner | Connects to service databases and inserts seed data |

Note: Inventory Service should avoid blocking calls. Prefer R2DBC for PostgreSQL
and reactive Redis APIs. Kafka can stay with Spring Kafka first if needed, but
blocking work should not be placed directly on the Netty event loop.

---

# Services

| Service | Responsibility |
| --- | --- |
| Product | Product catalog, product detail/list/search, product cache |
| Inventory | Stock check, stock reservation, stock release |
| Order | Order creation, order status lifecycle |
| Payment | Fake payment processing |
| Notification | Email simulation |
| Analytics | Build metrics/read models from business events |
| Audit | Persist important business events for trace/debug/history |
| Seed | Run seed data scripts against service databases |

Each service owns its own database.

---

# Mono Repo Structure

```text
mini_app/
  settings.gradle
  build.gradle
  docker-compose.yml
  common/
  product_service/
  inventory_service/
  order_service/
  payment_service/
  notification_service/
  analytics_service/
  audit_service/
  seed_service/
```

Rules:

- Each service is an independent Spring Boot application.
- Each service has its own `application.yml`.
- Each service owns its own Liquibase changelog.
- Each service defines only its own schema/migrations.
- Seed data belongs to `seed_service`, not individual business services.
- Seed Service can connect to multiple service databases.
- Seed Service Liquibase files should use raw `<sql>` blocks for inserts.
- Shared DTOs/constants/utilities can live in `common`.
- Do not put service-specific business logic in `common`.
- Inventory Service is the only WebFlux service for now.
- Other services use WebMVC + JPA first for clarity.

---

# Kafka Topics

| Topic | Producer | Consumer |
| --- | --- | --- |
| order.created | Order | Inventory, Analytics, Audit |
| inventory.reserved | Inventory | Payment, Audit |
| inventory.failed | Inventory | Order, Analytics, Audit |
| payment.completed | Payment | Order, Analytics, Audit |
| payment.failed | Payment | Order, Inventory, Analytics, Audit |
| inventory.released | Inventory | Audit |
| order.paid | Order | Notification, Analytics, Audit |
| order.cancelled | Order | Notification, Analytics, Audit |

---

# Analytics Service

Analytics Service is not part of the critical checkout transaction. It consumes
Kafka events asynchronously and builds business-facing metrics/read models.

Examples:

- Total orders created
- Total paid orders
- Total cancelled orders
- Payment success/failure count
- Inventory reservation failure count
- Hot product ranking from order/payment events
- Revenue summary from `payment.completed`

This service is allowed to lag behind real time. If Analytics is down, checkout
should still work because Kafka keeps the events for later consumption.

---

# Audit Service

Audit Service stores important business events for traceability. It is used when
debugging, replaying a business flow, or checking what happened to an order.

Examples:

- Store event id, event type, aggregate id, payload, timestamp and source service
- Trace full order journey: `order.created` -> `inventory.reserved` ->
  `payment.completed` -> `order.paid`
- Investigate failure journey: `order.created` -> `inventory.failed` ->
  `order.cancelled`
- Support idempotency/debug checks when duplicate events appear

Audit keeps raw event history. Analytics keeps aggregated/reporting data. Do not
mix these two responsibilities.

---

# Redis

## Cache

- cache:product:{id}
- cache:product:list:{page}

## Lock

- lock:inventory:{productId}

## Idempotency

- idempotency:{eventId}

## Rate Limit

- ratelimit:ip:{ip}
- ratelimit:checkout:{clientId}

## Leaderboard

- hot-products

---

# Happy Path

1. Client calls Order Service directly.
2. Order Service validates order request.
3. Order Service saves Order with status `PENDING`.
4. Order Service publishes `order.created`.
5. Inventory Service reserves stock.
6. Inventory Service publishes `inventory.reserved`.
7. Payment Service processes fake payment.
8. Payment Service publishes `payment.completed`.
9. Order Service updates order status to `PAID`.
10. Order Service publishes `order.paid`.
11. Notification Service sends simulated email.
12. Analytics and Audit consume related events asynchronously.

---

# Compensation

```text
payment.failed
    ->
Inventory releases stock
    ->
inventory.released
    ->
Order -> CANCELLED
    ->
order.cancelled
```

---

# Flash Sale

```text
Acquire Redis Lock
    ->
Read inventory
    ->
Reserve stock
    ->
Commit database
    ->
Release lock
    ->
Publish inventory.reserved
```

---

# Outbox Pattern

Transaction:

- Insert business data, such as Order or Inventory reservation
- Insert Outbox Event

Background Publisher:

- Read unpublished events
- Publish Kafka event
- Mark event as published

---

# Retry

```text
Consumer Exception
    ->
Retry Topic
    ->
Retry 3 times
    ->
Dead Letter Queue
```

---

# Folder Structure

```text
docs/
  00-overview.md
  01-architecture.md
  02-service-runtime-choice.md
  03-product_service.md
  04-inventory_service.md
  05-order_service.md
  06-payment_service.md
  07-notification_service.md
  08-analytics_service.md
  09-audit_service.md
  10-kafka.md
  11-redis.md
  12-deployment.md
  13-roadmap.md
```

---

# Development Milestones

## Milestone 1

- Docker Compose
- PostgreSQL
- Redis
- Kafka

## Milestone 2

- Product Service
- Product CRUD
- Redis product cache

## Milestone 3

- Inventory Service
- Spring WebFlux
- Stock check
- Stock reservation

## Milestone 4

- Order Service
- Order creation
- Publish `order.created`

## Milestone 5

- Kafka messaging between Order and Inventory
- Inventory reservation result events

## Milestone 6

- Payment Service
- Payment Saga
- Compensation flow

## Milestone 7

- Notification Service
- Order paid/cancelled notification simulation

## Milestone 8

- Redis distributed lock for flash sale inventory reservation

## Milestone 9

- Retry + DLQ
- Idempotent Consumer

## Milestone 10

- Outbox Pattern

## Milestone 11

- Analytics
- Audit

## Milestone 12

- Monitoring
- Logging
- OpenTelemetry

---

# Deferred / Not In Current Scope

- API Gateway
- Spring Security
- JWT
- Register/Login
- Role-based authorization
- Frontend authentication flow

---

# Future

- Prometheus
- Grafana
- Zipkin
- Kubernetes
- CI/CD
- Contract Testing
- API Gateway, if service routing becomes necessary
- Authentication/authorization, when core business flow is stable
