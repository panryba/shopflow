    # CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**ShopFlow – Microservices Platform** — an online shop portfolio project demonstrating senior-level distributed systems patterns. Built with Quarkus 3.33, Java 25, Kafka, Avro, and PostgreSQL.

All planned features are complete: Docker Compose, API Gateway, Keycloak + JWT auth, Angular frontend, GitHub Actions CI/CD, Prometheus + Grafana + Loki observability, integration tests.

## Build & Run

Each service is an independent Maven project. Run from the service directory:

```bash
# Dev mode (spins up Kafka, PostgreSQL, Apicurio via Testcontainers automatically)
./mvnw quarkus:dev

# Build
./mvnw package

# Build native
./mvnw package -Pnative
```

Run tests with:
```bash
./mvnw test                        # all tests
./mvnw test -Dtest=MyTest          # single test
```

Dev mode profile: Hibernate drops and recreates schema, Flyway disabled. Prod profile: Hibernate validates, Flyway migrates.

## Services

| Service | Port | Group ID |
|---------|------|----------|
| order-service | 8080 | com.example.order |
| payment-service | 8081 | com.example.payment |
| inventory-service | 8082 | com.example.inventory |

Root `pom.xml` is a Maven aggregator (no shared dependencies — each service manages its own).

## Kafka Topics & Saga Flow

The order-service is the **saga orchestrator**. All inter-service communication is async via Kafka. Events are serialized with **Avro + Apicurio Schema Registry** (port 8091).

```
order-service  →  payment-request    →  payment-service
payment-service →  payment-completed  →  order-service
payment-service →  payment-failed     →  order-service

order-service  →  inventory-request  →  inventory-service
inventory-service → inventory-approved → order-service
inventory-service → inventory-rejected → order-service

order-service  →  payment-rollback   →  payment-service  (on inventory rejection or timeout)
```

All consumers have dead-letter queues (`<topic>-dlq`) with 5 retries and exponential backoff.

Saga steps: `WAITING_PAYMENT` → `WAITING_INVENTORY` → `COMPLETED` / `CANCELLED`

## Avro Schemas

Each service owns its schema files under `src/main/avro/`:
- `consumed/` — schemas for events this service reads
- `produced/` — schemas for events this service writes

All schemas share the namespace `com.example.order.events.avro`. Avro classes are generated into `target/generated-sources/avsc/`. Never edit generated classes directly.

When adding a new event: add `.avsc` in both the producing service's `produced/` and the consuming service's `consumed/` directories.

## order-service Architecture (Hexagonal)

```
domain/           — pure domain model, no framework deps
  model/          — Order, OrderItem, OrderStatus, Money
  event/          — domain event records (PaymentRequestEvent, etc.)
  valueobject/    — OrderId

application/
  port/input/     — OrderUseCase (interface)
  port/output/    — OrderEventPublisher (interface), OrderRepository (interface)
  saga/           — OrderSagaOrchestrator, OrderSagaState, OrderSagaRepository, SagaTimeoutJob

infrastructure/
  messaging/      — KafkaOrderEventPublisher, OrderEventConsumer
  outbox/         — OutboxService, OutboxPublisherJob, OutboxEventType
  inbox/          — InboxService
  observability/  — CorrelationIdProvider, CorrelationIdFilter

presentation/
  dto/            — CreateOrderRequest
  rest/           — OrderResource
```

payment-service and inventory-service are thin: a single `*EventConsumer` class per service, with runtime-toggleable behavior driven by the frontend's admin panel (`AdminComponent`) via admin-only REST endpoints (`PUT /payment/mode` / `PUT /inventory/mode`, proxied through the gateway as `PUT /api/payment/mode` / `PUT /api/inventory/mode`) — no code change or restart needed. Three independent flags per consumer:
- **Acceptance mode** — mutable `accepted` flag (default `true`), set via `setAccepted()`. Decides accept vs. reject for every message processed while it's set.
- **Delay** — artificial sleep before processing each message.
- **Failure simulation ("crash")** — forces the consumer to throw on every message, triggering the DLQ after 5 retries; the saga times out rather than reaching a normal terminal state.

## Key Patterns

**Outbox:** `OrderSagaOrchestrator` never calls Kafka directly. It writes to the `outbox` table in the same transaction as the domain change. `OutboxPublisherJob` (scheduled) reads pending outbox rows and publishes via `KafkaOrderEventPublisher`, then marks them sent. `OutboxEventType` enum maps to the correct publisher method.

**Inbox:** Every Kafka handler in `OrderEventConsumer` calls `inbox.receive(eventId, type)` first. If the event was already processed it returns false and the handler exits immediately — guarantees idempotency on redelivery.

**Saga timeout:** `SagaTimeoutJob` runs every 10 seconds. It finds sagas whose `deadline` has passed (30s per step). `WAITING_PAYMENT` → cancel order. `WAITING_INVENTORY` → cancel order + publish `payment-rollback`.

**Correlation ID:** `CorrelationIdFilter` extracts `X-Correlation-ID` from incoming HTTP headers and stores it in a request-scoped `CorrelationIdProvider`. Kafka consumers extract it from the `X-Correlation-ID` Kafka header. It is propagated on all outgoing Kafka messages and logged via MDC (`corrId` and `orderId` fields in every log line).

## Database (order-service only)

PostgreSQL. Flyway migrations in `src/main/resources/db/migration/`:
- V1: `orders`, `order_items`
- V2: `outbox`
- V3: (dropped in V6) processed events table
- V4: `order_saga` (columns: order_id, step, deadline, correlation_id, updated_at)
- V5: `inbox_events`
- V6: drops the old processed_events table

payment-service and inventory-service have no database.