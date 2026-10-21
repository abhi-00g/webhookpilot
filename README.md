# WebhookPilot

An event-driven webhook delivery platform with guaranteed at-least-once delivery, built with Java, Spring Boot, Apache Kafka, and React.

**Live:** [webhookpilot.vercel.app](https://webhookpilot.vercel.app) · **API:** [webhookpilot-backend.onrender.com](https://webhookpilot-backend.onrender.com)

> The backend runs on Render's free tier and spins down after inactivity. The first request after idle takes ~30 seconds to cold-start — subsequent requests are fast.

## What it does

WebhookPilot accepts events from your application via API, fans them out to every subscribed endpoint, and handles the hard parts: cryptographic signing, automatic retries with exponential backoff, circuit breaking for failing endpoints, rate limiting, and full delivery observability.

## Architecture

```
┌──────────────┐     ┌───────────────────────────────────────────────────────┐
│   Your App   │     │                     WebhookPilot                      │
│              │     │                                                       │
│  POST /ingest├────►│  REST API ──► PostgreSQL ──► Kafka Producer           │
│  X-API-Key   │     │                                    │                  │
└──────────────┘     │                              ┌─────▼──────┐           │
                     │                              │   Kafka    │           │
┌──────────────┐     │                              │  Consumer  │           │
│  Dashboard   │     │                              └────-─┬─────┘           │
│   (React)    ├────►│  JWT Auth                    Fan-out│                 │
│              │     │  Applications                       │                 │
│              │     │  Events                     ┌────-──▼──────┐          │
│              │     │  Endpoints                  │   Delivery   │          │
│              │     │  Analytics                  │    Worker    │          │
└──────────────┘     │                             └──┬───┬───┬── ┘          │
                     │                                │   │   │              │
                     │                    ┌───────────┘   │   └──────┐       │
                     │                    ▼               ▼          ▼       │
                     │               ┌────────┐    ┌──────────┐  ┌─────┐     │
                     │               │  HTTP  │    │  Retry   │  │ DLQ │     │
                     │               │  POST  │    │ Scheduler│  │     │     │
                     │               │ + HMAC │    │(database)│  │     │     │
                     │               └────────┘    └──────────┘  └─────┘     │
                     │                                                       │
                     │  Redis: Circuit Breaker State + Rate Limiting         │
                     └───────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL |
| Messaging | Apache Kafka (producer/consumer, DLQ) |
| Caching | Redis (circuit breaker state, rate limiting) |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Recharts |
| Testing | JUnit 5, Mockito, MockMvc |
| CI/CD | GitHub Actions, Docker |
| Deployment | Render (backend), Vercel (frontend), Neon (PostgreSQL), Upstash (Redis) |

## Key Features

**Event Delivery Pipeline**
- Event ingestion via API key authentication
- Kafka-powered fan-out to all subscribed endpoints
- HMAC-SHA256 payload signing (same scheme as Stripe/GitHub webhooks)
- Idempotency keys to prevent duplicate event processing

**Reliability**
- Exponential backoff retries: 10s → 30s → 1m → 5m → 15m (6 max attempts)
- Dead letter queue for exhausted deliveries
- Circuit breaker pattern: auto-disables endpoints after 5 consecutive failures, cooldown probe after 60s
- Sliding window rate limiting: 100 events/minute per application

**Observability**
- Structured JSON logging with MDC fields (eventId, endpointId, attemptNumber, deliveryStatus)
- Full delivery attempt history with HTTP status codes and response bodies
- Real-time dashboard with event activity charts and delivery status breakdown

**Dashboard**
- Application management with API key generation
- Event type and endpoint CRUD with subscription management
- Send test events directly from the UI
- Auto-refreshing event list with delivery status tracking
- Delivery attempt timeline on event detail view

## Kafka in Production

The architecture is built around Kafka for event fan-out and asynchronous delivery. Locally, the full Kafka pipeline runs via Docker Compose — events flow through the `webhook.events` topic to the consumer, which fans out to endpoints, with failed deliveries routed to the `webhook.events.dlq` dead letter queue.

In the deployed version, the application gracefully falls back to synchronous delivery when Kafka is unavailable. The delivery logic is identical (same fan-out, same HMAC signing, same retry scheduling) — only the trigger changes from Kafka consumer to direct method call. This is a deliberate design choice: the Kafka architecture is production-ready and the fallback ensures the platform works in any environment without requiring a managed Kafka service.

To run with the full Kafka pipeline locally:

```bash
docker compose up -d    # starts PostgreSQL, Kafka, and Redis
cd backend && ./mvnw spring-boot:run
```

## Getting Started

### Prerequisites

- Java 17+ ([download](https://adoptium.net/))
- Maven 3.9+ (`brew install maven` on macOS)
- Docker Desktop ([download](https://www.docker.com/products/docker-desktop/))
- Node.js 20+ ([download](https://nodejs.org/))

### Local Development

1. **Clone and start infrastructure:**

```bash
git clone https://github.com/abhi-00g/webhookpilot.git
cd webhookpilot
docker compose up -d
```

This starts PostgreSQL (port 5433), Kafka (port 9092), and Redis (port 6379).

2. **Start the backend:**

```bash
cd backend
mvn wrapper:wrapper   # first time only
./mvnw spring-boot:run
```

The API runs at `http://localhost:8080`.

3. **Start the frontend:**

```bash
cd frontend
npm install   # first time only
npm run dev
```

The dashboard runs at `http://localhost:5173`.

### Quick Test

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@test.com","password":"Password1!"}' | python3 -m json.tool

# Create an application (use the token from registration)
curl -s -X POST http://localhost:8080/api/applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Demo App"}' | python3 -m json.tool

# Create an event type
curl -s -X POST http://localhost:8080/api/applications/<APP_ID>/event-types \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"order.created","description":"New order placed"}'

# Ingest an event (uses API key, not JWT)
curl -s -X POST http://localhost:8080/api/events/ingest \
  -H "Content-Type: application/json" \
  -H "X-API-Key: <API_KEY>" \
  -d '{"eventType":"order.created","payload":{"orderId":"123","amount":49.99}}'
```

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | None | Create account, returns JWT |
| POST | `/api/auth/login` | None | Sign in, returns JWT |

### Applications

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/applications` | JWT | Create application |
| GET | `/api/applications` | JWT | List applications |
| GET | `/api/applications/:id` | JWT | Get application |
| DELETE | `/api/applications/:id` | JWT | Delete application |

### Event Types

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/applications/:appId/event-types` | JWT | Create event type |
| GET | `/api/applications/:appId/event-types` | JWT | List event types |
| DELETE | `/api/applications/:appId/event-types/:id` | JWT | Delete event type |

### Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/applications/:appId/endpoints` | JWT | Register endpoint |
| GET | `/api/applications/:appId/endpoints` | JWT | List endpoints |
| PATCH | `/api/applications/:appId/endpoints/:id` | JWT | Update endpoint |
| DELETE | `/api/applications/:appId/endpoints/:id` | JWT | Delete endpoint |
| PUT | `/api/applications/:appId/endpoints/:id/event-types` | JWT | Set subscriptions |
| GET | `/api/applications/:appId/endpoints/:id/event-types` | JWT | Get subscriptions |

### Events

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/events/ingest` | API Key | Ingest event |
| GET | `/api/applications/:appId/events` | JWT | List events (paginated) |
| GET | `/api/applications/:appId/events/:id` | JWT | Get event with delivery attempts |

### Webhook Delivery Headers

Every webhook delivery includes these headers:

| Header | Description |
|--------|-------------|
| `X-Webhook-Id` | Event UUID for deduplication |
| `X-Webhook-Timestamp` | Unix timestamp of signature |
| `X-Webhook-Signature` | `sha256=<hmac_hex>` for payload verification |
| `User-Agent` | `WebhookPilot/1.0` |

**Verifying signatures:** Compute `HMAC-SHA256(timestamp + "." + raw_body, endpoint_secret)` and compare with the signature header value.

## Running Tests

```bash
cd backend
./mvnw test
```

17 tests across 3 test classes:
- `HmacSignatureServiceTest` — HMAC-SHA256 correctness, determinism, replay attack prevention
- `AuthServiceTest` — registration, login, duplicate email handling, password hashing
- `RateLimiterServiceTest` — rate limit enforcement, TTL behavior

## Project Structure

```
webhookpilot/
├── backend/
│   ├── Dockerfile
│   ├── src/main/java/com/webhookpilot/
│   │   ├── config/          # Security, Kafka, Redis, delivery, rate limit config
│   │   ├── controller/      # REST endpoints (auth, apps, events, endpoints)
│   │   ├── dto/             # Request/response DTOs, Kafka messages
│   │   ├── exception/       # Global error handling, rate limit exception
│   │   ├── model/           # JPA entities (7 tables)
│   │   ├── repository/      # Spring Data JPA interfaces
│   │   ├── security/        # JWT filter, token provider, user details service
│   │   └── service/         # Business logic, Kafka producer/consumer,
│   │                        # delivery worker, circuit breaker, rate limiter,
│   │                        # HMAC signing, retry scheduler
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-prod.yml
│       └── logback-spring.xml
├── frontend/
│   └── src/
│       ├── api/             # API client with JWT management
│       ├── components/      # Layout, UI components, toast system
│       ├── context/         # Auth context
│       └── pages/           # Login, Applications, AppDetail, EventDetail
├── docker-compose.yml       # PostgreSQL, Kafka, Redis for local dev
└── .github/workflows/ci.yml # Tests + build on every push/PR
```

## License

MIT
