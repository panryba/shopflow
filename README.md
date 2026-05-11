# ShopFlow – Microservices Platform

![Java](https://img.shields.io/badge/Java-25-orange) ![Quarkus](https://img.shields.io/badge/Quarkus-3.33-blueviolet) ![Kafka](https://img.shields.io/badge/Kafka-4.1.1-black) ![Avro](https://img.shields.io/badge/Avro-1.12.1-critical) ![Apicurio](https://img.shields.io/badge/Apicurio-3.1.7-orangered) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue) ![Angular](https://img.shields.io/badge/Angular-21-red) ![Keycloak](https://img.shields.io/badge/Keycloak-26-teal) ![Docker](https://img.shields.io/badge/Docker-Compose-blue) [![CI/CD](https://github.com/panryba/shop-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/panryba/shop-microservices/actions/workflows/ci.yml)

A production-shaped online shop built as a microservices portfolio project, demonstrating senior-level distributed systems patterns: **Hexagonal Architecture**, **Domain-Driven Design**, **Saga Orchestrator**, **Transactional Outbox**, **Idempotent Consumer (Inbox)**, **Dead Letter Queue**, **Saga Timeout**, **Avro + Schema Registry**, **Partition Key Consistency**, **Correlation ID Tracing**, **Concurrency Control**, **Idempotent Order Creation**, **API Gateway**, **Fault Tolerance**, and **JWT Authentication**.

---

## Quick Start

> Requires [Docker](https://docs.docker.com/get-docker/) and Docker Compose. No Java, Maven, or Node installation needed.

```bash
git clone https://github.com/panryba/shop-microservices.git
cd shop-microservices
docker compose up
```

| Endpoint | URL |
|----------|-----|
| API Gateway | http://localhost:8090 |
| Gateway Swagger UI | http://localhost:8090/q/swagger-ui |
| Keycloak Admin | http://localhost:8180/admin (admin / admin) |
| Frontend | http://localhost:4200 |

---

## Architecture

```mermaid
graph TB
    subgraph Client
        UI[Angular Frontend]
    end

    subgraph Auth["Auth (Keycloak)"]
        KC[Keycloak<br/>OIDC / JWT]
    end

    subgraph Gateway["API Gateway (Quarkus)"]
        GW[Request Router<br/>+ JWT Validation]
    end

    subgraph Services
        OS["Order Service :8080<br/>(Saga Orchestrator)"]
        PS[Payment Service :8081]
        IS[Inventory Service :8082]
    end

    subgraph Messaging["Event Bus"]
        K[Apache Kafka]
        SR[Apicurio<br/>Schema Registry]
    end

    subgraph Storage
        ODB[(Order DB<br/>PostgreSQL)]
    end

    UI <-->|OIDC login| KC
    UI -->|HTTPS + JWT| GW
    GW -->|validate token| KC
    GW -->|route| OS
    GW -->|route| IS

    OS <--> ODB
    OS -->|payment-request<br/>payment-rollback| K
    OS -->|inventory-request| K
    K -->|payment-completed<br/>payment-failed| OS
    K -->|inventory-approved<br/>inventory-rejected| OS

    K -->|payment-request<br/>payment-rollback| PS
    PS -->|payment-completed<br/>payment-failed| K

    K -->|inventory-request| IS
    IS -->|inventory-approved<br/>inventory-rejected| K

    K <-->|Avro schema lookup| SR
```

---

## Patterns Implemented

### Hexagonal Architecture (Ports & Adapters)
The `order-service` is structured in three layers with strict dependency direction (inward only):
- **Domain** — pure Java, no framework dependencies
- **Application** — use cases and saga orchestration, depends only on domain
- **Infrastructure** — Kafka, JPA, outbox, inbox; implements the output ports defined by the application layer

Payment and inventory services are intentionally thin — their sole responsibility is to simulate an external system responding to events.

### Domain-Driven Design
The `order-service` domain layer models the business explicitly: `Order` is the aggregate root, `OrderItem` is a child entity, `Money` and `OrderId` are value objects, and `OrderStatus` is a state enum. No Quarkus, JPA, or Kafka annotation touches this layer. Infrastructure concerns (persistence, messaging) implement ports defined by the application layer and depend inward — never the other way around.

### Saga Orchestrator
The `order-service` owns the entire order lifecycle and drives every step explicitly. It decides what happens next based on each response — no choreography, no implicit coupling between services. The saga state (`WAITING_PAYMENT` → `WAITING_INVENTORY` → `COMPLETED` / `CANCELLED`) is persisted in the database, making it recoverable after a restart.

### Transactional Outbox
The orchestrator never publishes to Kafka directly inside a business transaction. Instead it writes an `outbox` row in the same transaction as the domain change. A scheduled `OutboxPublisherJob` reads pending rows and publishes them to Kafka, then marks them sent. This eliminates the dual-write problem: if the service crashes after committing the DB transaction but before publishing, the outbox row survives and will be retried. Outbox publishing is idempotent — retries may result in duplicate sends, which are handled by idempotent consumers (Inbox pattern).

### Idempotent Consumer (Inbox)
The system operates under at-least-once delivery semantics — all consumers must be idempotent. Every Kafka event handler records the `eventId` in an `inbox_events` table before processing. On redelivery, the duplicate is detected and silently skipped. This makes all consumers safe to retry without risk of double-charging, double-approving, or double-cancelling.

### Dead Letter Queue
Each consumer channel is configured with `failure-strategy=dead-letter-queue`. Retries are handled via application-level `@Retry` and Kafka redelivery. After repeated failures, a poisoned message is moved to a `<topic>-dlq` topic and the consumer continues. Nothing blocks.

### Saga Timeout
A `SagaTimeoutJob` runs every 10 seconds and finds sagas where the step deadline has passed (30 seconds per step). Timed-out `WAITING_PAYMENT` sagas cancel the order. Timed-out `WAITING_INVENTORY` sagas cancel the order and trigger a `payment-rollback` to reverse the charge. This prevents orders from being stuck in a pending state forever if a downstream service is unavailable.

### Avro + Schema Registry
All Kafka messages are serialized with Apache Avro against schemas registered in Apicurio Schema Registry. Schemas are auto-registered on first publish. Each service owns its schema files under `src/main/avro/consumed/` and `src/main/avro/produced/`. This enforces a contract between producers and consumers and enables schema evolution without breaking existing consumers.

### Partition Key Consistency
Every outgoing Kafka message is keyed by `orderId`. Kafka guarantees that all messages with the same key are routed to the same partition and consumed in order. This means all events for a single order — `payment-request`, `payment-completed`, `inventory-request`, `inventory-approved` — are processed sequentially by the consumer, with no risk of out-of-order state transitions.

### Correlation ID Tracing
Every request receives an `X-Correlation-ID` header (generated if absent). It is propagated as a Kafka record header on every outgoing event and extracted by every consumer. All log lines include `corrId` and `orderId` via MDC, making it possible to trace a single order flow across synchronous HTTP requests and asynchronous saga events.

### Concurrency Control
All REST handlers and Kafka consumers in the `order-service` are annotated with `@Retry(retryOn = OptimisticLockException.class)`. If two concurrent requests attempt to update the same order or saga row simultaneously, JPA throws an `OptimisticLockException` and the operation is retried automatically with jitter. This prevents silent data corruption under concurrent load without resorting to pessimistic locking.

### Idempotent Order Creation
`POST /orders` accepts an optional `Idempotency-Key: <uuid>` header. The client generates the UUID before sending and retries safely if the network times out — the order-service checks whether that key was already processed and returns the existing order ID instead of creating a duplicate. The key is stored as a unique-constrained column on the `orders` table. A concurrent duplicate (race condition where two requests pass the pre-check simultaneously) is caught via `PersistenceException` cause-chain inspection, with the fallback lookup running in a separate `REQUIRES_NEW` transaction to bypass the poisoned outer transaction. The key is echoed back in the response `Idempotency-Key` header.

### API Gateway
A dedicated Quarkus service (port 8090) acts as the single entry point for all clients. It routes `/api/orders/**` to the order-service and `/api/inventory/mode` to the inventory-service via typed MicroProfile REST Client proxy interfaces. The gateway generates or propagates `X-Correlation-ID` on every inbound request (server-side `ContainerRequestFilter`) and attaches it to every outgoing downstream call (client-side `ClientRequestFilter` registered via `@RegisterProvider`). Downstream responses are rebuilt before returning to the client — hop-by-hop headers (`transfer-encoding`, `content-length`, `host`, `connection`) are stripped to prevent HTTP framing conflicts. Unreachable downstream services map to `502 Bad Gateway`; open circuit returns `503 Service Unavailable`; 4xx errors from downstream pass through with their original status code.

### Fault Tolerance
All gateway-to-downstream calls are protected by MicroProfile Fault Tolerance. Read and idempotent write operations use `@Retry(maxRetries = 3, delay = 200ms)` — retries abort immediately on `WebApplicationException` so 4xx responses are never retried. POST (create order) gets no retry as the operation is not idempotent. All operations are protected by `@CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5s, successThreshold = 2)` — after 50% failures across 10 requests the circuit opens for 5 seconds; once open, requests fail fast with `503 SERVICE_UNAVAILABLE` and a structured `GatewayErrorResponse` without hitting the downstream service. REST clients are configured with a 1s connect timeout and 3s read timeout to bound worst-case latency on the local/Docker network.

### JWT Authentication
JWT signature validation is enforced at the gateway only. Downstream services trust the forwarded token and use it only for identity extraction and authorization context. The gateway uses `quarkus-oidc` with Keycloak as the OIDC provider. Unauthenticated requests return `401`; insufficient role returns `403` — both as structured `GatewayErrorResponse` JSON.

The raw `Authorization: Bearer <token>` header is forwarded downstream via a `ClientRequestFilter` (`OutgoingJwtFilter`) so services can read user identity without performing OIDC validation against Keycloak again. Role-based access: order endpoints require any authenticated user; `PUT /api/inventory/mode` requires role `admin`. The `order-service` extracts customer identity directly from the forwarded token — `customerId` is never trusted from the request body.

In the current Keycloak configuration, the `sub` claim is a UUID and is used as the authoritative customer identity for every order.

---

## Delivery Guarantees

| Concern | Guarantee |
|---------|-----------|
| Messaging | At-least-once |
| Outbox | Eventual delivery — survives crashes, retried until published |
| Inbox | Idempotent processing — duplicates detected and discarded |
| Saga | Eventual consistency — every step is recoverable or compensated |

---

## Services

| Service | Port | Responsibility |
|---------|------|----------------|
| gateway | 8090 | API Gateway — single entry point, routes to downstream services, propagates Correlation ID |
| order-service | 8080 | Saga orchestrator, order lifecycle, REST API |
| payment-service | 8081 | Simulates payment processing |
| inventory-service | 8082 | Simulates inventory availability check |
| frontend | 4200 | Angular SPA (served by nginx in Docker) |

---

## Saga Flow

### Happy Path

```
Client           Order Service        Payment Service    Inventory Service
  |                    |                    |                   |
  |-- POST /orders --> |                    |                   |
  |                    |-- payment-request->|                   |
  |                    |<-payment-completed-|                   |
  |                    |-- inventory-request------------------->|
  |                    |<-inventory-approved--------------------|
  |                    |                                        |
  |              status: COMPLETED                              |
```

### Payment Failure

```
Order Service  →  payment-request  →  Payment Service
               ←  payment-failed   ←
Order cancelled, no rollback needed (payment never charged)
```

### Inventory Rejection

```
Order Service  →  inventory-request  →  Inventory Service
               ←  inventory-rejected ←
Order Service  →  payment-rollback   →  Payment Service
Order cancelled, payment reversed
```

### Timeout

```
SagaTimeoutJob detects deadline exceeded (every 10s, 30s deadline per step)
  WAITING_PAYMENT   → cancel order
  WAITING_INVENTORY → cancel order + payment-rollback
```

### Simulating Failures

All endpoints require a valid JWT. Obtain a token first:

```bash
# user1 token (role: user) — for order endpoints
TOKEN=$(curl -s -X POST http://localhost:8180/realms/shopflow/protocol/openid-connect/token \
  -d "grant_type=password&client_id=shopflow-app&username=user1&password=password" \
  | jq -r .access_token)

# admin token (role: admin) — required for inventory mode toggle
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8180/realms/shopflow/protocol/openid-connect/token \
  -d "grant_type=password&client_id=shopflow-app&username=admin&password=password" \
  | jq -r .access_token)
```

| Scenario | How                                                                                                         |
|----------|-------------------------------------------------------------------------------------------------------------|
| Payment failure | Place an order where total exceeds 1000                                                                     |
| Inventory rejection | `PUT http://localhost:8090/api/inventory/mode?accept=false` <br/>(admin token required)<br/>then place any order |
| Restore inventory acceptance | `PUT http://localhost:8090/api/inventory/mode?accept=true` <br/>(admin token required)                      |

---

## API Reference

All endpoints are served by the **API Gateway** at `http://localhost:8090/api`.

All endpoints require a `Bearer` token in the `Authorization` header. Order endpoints require role `user` or `admin`; inventory mode toggle requires role `admin`.

Swagger UI available at `http://localhost:8090/q/swagger-ui` in dev mode.

### POST /api/orders
Place a new order. Starts the saga asynchronously.

**Request**
```json
{
  "items": [
    { "productId": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "quantity": 2, "price": 74.99 }
  ]
}
```

> `customerId` is not accepted from the client — it is extracted from the `sub` claim of the forwarded JWT token.

> `price` per item is accepted from the client as a pragmatic simplification — in a production system it would be fetched from a product catalog service and never trusted from the client.

**Response** `202 Accepted`
```
Location: /orders/{orderId}
```

### GET /api/orders
Returns all orders.

### GET /api/orders/{id}
Returns a single order.

**Response** `200 OK`
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "items": [
    { "productId": "7c9e6679-7425-40de-944b-e07fc1f90ae7", "quantity": 2, "price": 74.99 }
  ],
  "total": 149.98
}
```

**Order statuses:** `PENDING` → `PAID` → `COMPLETED` / `CANCELLED`

### PUT /api/orders/{id}/cancel
Manually cancel an order.

**Response** `200 OK` — returns the updated order with `status: CANCELLED`

---

## Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `payment-request` | order-service | payment-service |
| `payment-completed` | payment-service | order-service |
| `payment-failed` | payment-service | order-service |
| `payment-rollback` | order-service | payment-service |
| `inventory-request` | order-service | inventory-service |
| `inventory-approved` | inventory-service | order-service |
| `inventory-rejected` | inventory-service | order-service |

Each topic has a corresponding DLQ: `<topic>-dlq`.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Quarkus 3.33, Java 25 |
| Frontend | Angular 21, nginx |
| Messaging | Apache Kafka 4.1.1, SmallRye Reactive Messaging |
| Serialization | Apache Avro 1.12.1, Apicurio Schema Registry 3.1.7 |
| Database | PostgreSQL 18, Hibernate ORM Panache, Flyway |
| Resilience | MicroProfile Fault Tolerance (retry, DLQ) |
| Auth | Keycloak 26, quarkus-oidc, MicroProfile JWT |
| API | JAX-RS, OpenAPI / Swagger UI |
| Infrastructure | Docker, Docker Compose, GitHub Actions |

---

## CI/CD

Every push to `master` triggers the pipeline. Pull requests run build and test only — no push to Docker Hub.

```mermaid
graph LR
    subgraph parallel["Parallel"]
        B["build-backend<br/>─────────────<br/>Java 25 + Maven cache<br/>mvn package -DskipTests<br/>mvn verify -DskipCompile<br/>(when tests exist)<br/>save to cache"]
        F["build-frontend<br/>─────────────<br/>Node 24<br/>npm ci<br/>npm run build<br/>npm test<br/>(when tests exist)"]
    end

    D["docker-push<br/>─────────────<br/>restore from cache<br/>build 5 images<br/>push to Docker Hub<br/>(master only)"]

    B --> D
    F --> D
```

Images pushed: `tbzowka/{order-service,payment-service,inventory-service,gateway,frontend}:latest`

---

## Roadmap

- [x] **Docker Compose** — single `docker-compose up` to run all services, Kafka, Apicurio, PostgreSQL
- [x] **GitHub Actions CI/CD** — build, test, push Docker images to Docker Hub on merge to master
- [x] **API Gateway** — Quarkus REST Client proxy, single entry point, Correlation ID propagation, 502 error handling
- [x] **Authentication** — Keycloak OIDC, JWT validation at gateway, role-based access control, JWT forwarded downstream
- [ ] **Angular Frontend** — product listing, shopping cart, checkout, real-time order status via SSE
- [ ] **Integration Tests** — `@QuarkusTest` + Testcontainers, happy path saga E2E, inbox idempotency verification
- [ ] **Observability** — Prometheus metrics, Grafana dashboard; key metrics: outbox lag, DLQ size, saga duration, order throughput