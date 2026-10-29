# Distributed URL Shortener & Analytics Platform

[![CI Pipeline](https://github.com/DmytroChup/url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/DmytroChup/url-shortener/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green?logo=springboot)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud-Gateway-blue)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-green?logo=springsecurity)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-blue?logo=typescript)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38B2AC?logo=tailwind-css)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-4.3-red?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-8.8-red?logo=redis)
![Nginx](https://img.shields.io/badge/Nginx-Edge_Proxy-009639?logo=nginx)
![Docker](https://img.shields.io/badge/Docker%20Compose-Ready-blue?logo=docker)

A distributed URL shortening and clickstream analytics platform built with Spring Boot, Apache Kafka, Redis, PostgreSQL, and a modern React 19 SPA.

The service separates the latency-critical redirect path from event processing using the **Transactional Outbox Pattern**, incorporates **distributed JWT authentication with Redis refresh token rotation**, **Resilience4j circuit breaking**, **idempotent Kafka consumption**, and **distributed tracing via OpenZipkin**.

---

## Web Dashboard Preview

![Web Interface Preview](assets/web-ui.png)

*Developer-first SPA interface featuring dark OLED aesthetics, user authentication, personal link management ("My Links"), QR code generation, configurable TTL link retention, and real-time click analytics charts.*

---

## Architecture

![System Architecture](assets/architecture.png)

The platform is decomposed into five primary services:

* **Frontend & Edge Reverse Proxy (`:80`)**: Nginx reverse proxy serving the compiled React 19 SPA. Serves as the single unified public entry point, proxies API calls and short link redirects to the API Gateway, caches static assets, and strips client-spoofed `X-Forwarded-For` headers.
* **API Gateway (`:8080`)**: Built on Spring Cloud Gateway (WebFlux / Netty). Manages path routing, central JWT authentication via `JwtAuthenticationFilter`, anti-spoofing header sanitization (`X-User-Id` / `X-User-Role`), Redis Token Bucket rate limiting, and circuit breaking for downstream analytics requests.
* **Auth Service (`:8083`)**: Manages user registration, BCrypt password hashing, JWT access token generation, and stateful refresh token rotation with Token Family tracking and replay attack detection backed by Redis.
* **URL Service (`:8081`)**: Manages Base62 link generation, optional user ownership association, Redis cache-aside lookups, user link dashboard queries (`GET /api/v1/urls/me`), and transactional persistence of click events into an Outbox table.
* **Analytics Service (`:8082`)**: Inter-service owner authorization via `RestClient` to `url-service` (with Caffeine caching), clickstream consumption from Kafka, idempotent processing via unique event identifiers, GeoIP/User-Agent heuristic enrichment (device, OS, browser), Dead Letter Topic routing, and aggregated statistics.
* **Contracts Library (`:contracts`)**: Shared module defining domain events (`UrlClickEvent`), DTOs, and internal security headers (`SecurityHeaders`).

---

## Distributed Systems & Resilience Patterns

### 1. Unified Edge Entrypoint & Anti-Spoofing
All external traffic terminates at the Nginx edge proxy on port `80`. Nginx overrides incoming `X-Forwarded-For` headers with `$remote_addr` (socket IP), neutralizing client IP spoofing before requests reach the internal Spring Cloud Gateway and rate limiters.

### 2. Gateway Perimeter Security & Header Sanitization
Downstream microservices trust internal headers (`X-User-Id`, `X-User-Role`) propagated by the API Gateway. To eliminate header spoofing vulnerabilities from external clients, `api-gateway` strips any client-provided identity headers before token verification. Only cryptographically verified JWT claims are forwarded into the private network.

### 3. Distributed Authentication & Token Rotation (Token Families)
Authentication uses short-lived stateless JWT access tokens (15m TTL) paired with single-use refresh tokens stored in Redis.
* **Token Rotation**: Every refresh request invalidates the old refresh token and issues a new pair.
* **Concurrency Grace Period**: A 10-second window permits concurrent in-flight browser requests to reuse the previous token safely during network delays.
* **Replay Attack Detection**: If a revoked refresh token is reused outside the grace period, the entire token family is invalidated immediately in Redis, terminating all compromised user sessions.

### 4. Inter-Service Owner Authorization & Fail-Closed Policy
To prevent Insecure Direct Object References (IDOR), `analytics-service` validates that the requesting `X-User-Id` is the owner of the target short code by querying `url-service` via `RestClient`. Results are cached in Caffeine (5m TTL) to minimize inter-service overhead. If `url-service` is unreachable or returns an error, the endpoint defaults to a **fail-closed** policy (HTTP 403 Forbidden).

### 5. Transactional Outbox (Dual-Write Prevention)
Writing to PostgreSQL and publishing to Kafka within separate calls introduces potential state divergence if the network fails midway.
On every redirect, `UrlClickEvent` is stored inside the local `outbox_events` table within the same database transaction. An asynchronous worker polls unpublished events, pushes them to the `url-clicks` Kafka topic, and marks them processed upon broker acknowledgement.

### 6. Idempotent Consumer
Kafka provides at-least-once delivery semantics, meaning network retries or consumer group rebalances can cause duplicated event delivery.
The consumer tracks incoming `eventId` (UUID) values within a `processed_events` table. Duplicate events are acknowledged and discarded without updating click counts.

### 7. Dead Letter Topic (DLT / Poison Pill Isolation)
Unparseable payloads or unexpected deserialization errors can block partition offsets and halt consumer progress.
A custom `DefaultErrorHandler` with exponential backoff intercepts failing records and redirects unrecoverable events to `url-clicks.DLT`, allowing the main consumer loop to proceed uninterrupted.

### 8. Circuit Breaker & Fallback
To prevent cascading service degradation, the API Gateway protects analytics routes with **Resilience4j**. If downstream failure rates exceed 50%, the gateway opens the circuit and immediately responds with an **RFC 7807 Problem Detail (503 Service Unavailable)** fallback without blocking worker threads.

### 9. Cache Penetration Protection
Direct database queries for non-existent short codes can exhaust database connections under high traffic or scanning attacks.
When a short code is not found, a negative cache entry (`__NOT_FOUND__`) is written to Redis with a short 5-minute TTL, absorbing subsequent misses directly in memory.

### 10. Distributed Tracing
All HTTP requests, reactive Gateway filters, and asynchronous Kafka messages propagate W3C/B3 trace headers instrumented via **Micrometer Tracing** and **Brave**, with visualization in **OpenZipkin**.

---

## Repository Layout

```
url-shortener/
├── frontend/            # React 19 SPA, TypeScript, Tailwind CSS v4, Lucide icons, Recharts
├── api-gateway/         # Reactive gateway, JWT auth filter, anti-spoofing, rate limiting, circuit breaker
├── contracts/           # Shared domain events (UrlClickEvent), DTOs, and SecurityHeaders
├── auth-service/        # User identity, BCrypt, JWT issuance, Redis refresh token rotation
├── url-service/         # Base62 shortening, link ownership, Redis cache-aside, Outbox publisher
├── analytics-service/   # Kafka consumer, owner authorization check, idempotent processing, DLT routing
├── docker/              # Multi-database PostgreSQL initialization script (init-databases.sql)
├── load-tests/          # k6 load testing benchmark scripts
├── .github/workflows/   # CI/CD pipeline (Gradle tests, frontend typecheck, Docker image verification)
├── compose.yaml         # Production perimeter (port 80 only)
└── compose.dev.yaml     # Local dev overlay (loopback ports for DBs, Swagger, Zipkin)
```

---

## Quickstart

### Prerequisites
* Docker & Docker Compose

### 1. Production Mode (Hardened Perimeter)
Only Nginx (port `80`) is exposed. All backend services and databases remain private inside the Docker network:
```bash
docker compose up --build -d
```
Access the application at `http://localhost`.

### 2. Development Mode (With Debugging Ports)
Uses `compose.dev.yaml` to bind debugging ports strictly to `127.0.0.1` (loopback):
```bash
docker compose -f compose.yaml -f compose.dev.yaml up --build -d
```

### Endpoints & Infrastructure

| Service               | Public Port (Prod) | Dev Port (Loopback) | Description                             | Docs / UI                               |
|-----------------------|--------------------|---------------------|-----------------------------------------|-----------------------------------------|
| **Frontend & Edge**   | `80`               | `80`                | React SPA & Nginx Edge Reverse Proxy    | `http://localhost/`                     |
| **API Gateway**       | *Internal*         | `127.0.0.1:8080`    | Internal Gateway, JWT Auth & Rate Limit | Root routing                            |
| **Auth Service**      | *Internal*         | `127.0.0.1:8083`    | JWT issuance, token rotation & users    | REST API / Actuator                     |
| **URL Service**       | *Internal*         | `127.0.0.1:8081`    | Base62 shortening & Outbox publisher    | `http://localhost:8081/swagger-ui.html` |
| **Analytics Service** | *Internal*         | `127.0.0.1:8082`    | Clickstream consumer & reporting        | `http://localhost:8082/swagger-ui.html` |
| **Zipkin UI**         | *Internal*         | `127.0.0.1:9411`    | Distributed tracing dashboard           | `http://localhost:9411/zipkin/`         |
| **PostgreSQL**        | *Internal*         | `127.0.0.1:5444`    | Databases: `url_shortener`, `url_shortener_analytics`, `url_shortener_auth` | Mapped to internal `5432` |
| **Apache Kafka**      | *Internal*         | `127.0.0.1:9094`    | KRaft single-node broker                | Port `9092` internal                    |
| **Redis**             | *Internal*         | `127.0.0.1:6379`    | Cache-aside, rate limits & token family sessions | Standalone                    |

---

## API Verification (cURL)

Requests should be sent directly to the single entry point (`http://localhost`):

### 1. Register & Login
```bash
# Register
curl -X POST http://localhost/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "Secur3Password!"}'

# Login
curl -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "Secur3Password!"}'
```
Response:
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "uG8x_9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### 2. Shorten a URL (Authenticated or Anonymous)
```bash
curl -X POST http://localhost/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"url": "https://spring.io", "ttlDays": 30}'
```
Response:
```
"aB7xK9q"
```

### 3. Retrieve User's Links (Protected)
```bash
curl http://localhost/api/v1/urls/me \
  -H "Authorization: Bearer <access_token>"
```

### 4. Follow Redirect (HTTP 302)
```bash
curl -i http://localhost/aB7xK9q
```
Response:
```http
HTTP/1.1 302 Found
Location: https://spring.io
```

### 5. Check Analytics (Owner Protected)
```bash
curl http://localhost/api/v1/analytics/aB7xK9q \
  -H "Authorization: Bearer <access_token>"
```
Response:
```json
{
  "shortCode": "aB7xK9q",
  "totalClicks": 1,
  "topCountries": {
    "US": 1
  },
  "devices": {
    "DESKTOP": 1
  },
  "topBrowsers": {
    "Chrome": 1
  },
  "topOperatingSystems": {
    "Windows": 1
  }
}
```

### 6. Refresh Token Rotation
```bash
curl -X POST http://localhost/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "<refresh_token>"}'
```

---

## Load Testing & Benchmarks

The redirect engine was benchmarked using **Grafana k6** running inside the Docker network against the core service under a 50-second ramping profile with zero-redirect tracking:

| Metric               | Result           | Notes                                                        |
|----------------------|------------------|--------------------------------------------------------------|
| **Throughput (RPS)** | **3,891+ req/s** | Peak sustained redirect rate                                 |
| **Total Requests**   | **220,821**      | Total operations completed during test run                   |
| **Success Rate**     | **100.00%**      | 220,811 / 220,811 successful HTTP 302 redirects (0 failures) |
| **P50 Latency**      | **13.4 ms**      | Median response time                                         |
| **P90 Latency**      | **22.2 ms**      | 90th percentile latency                                      |
| **P95 Latency**      | **28.7 ms**      | 95th percentile latency                                      |
| **P99 Latency**      | **40.8 ms**      | 99th percentile (includes PostgreSQL Outbox persistence)     |

```powershell
# Run the benchmark
cat load-tests/redirect-benchmark.js | docker run --rm -i --network url-shortener_default -e TARGET_URL=http://url-service:8081 grafana/k6 run -
```

---

## Testing & CI/CD

* **Unit Testing**: Business logic isolated with JUnit 5 and Mockito.
* **Integration Testing**: Multi-module suites tested against real PostgreSQL, Redis, and Kafka containers via **Testcontainers**.
* **Frontend Verification**: TypeScript typecheck and production bundle build with Vite (`tsc -b && vite build`).
* **CI Pipeline**: Automated build and verification on **GitHub Actions** (`.github/workflows/ci.yml`) using Java 25, Gradle build cache, Node.js 22, and parallel multi-stage Dockerfile verification for all 5 services.

```bash
# Run local test suite across all modules
./gradlew test

# Build frontend
cd frontend && npm run build
```

---

## License
This project is open-source and available under the [MIT License](LICENSE).
