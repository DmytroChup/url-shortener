# ⚡ Distributed URL Shortener & Real-Time Analytics Platform

[![CI Pipeline](https://github.com/DmytroChup/url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/DmytroChup/url-shortener/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green?logo=springboot)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud-Gateway-blue)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-4.3-red?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-8.8-red?logo=redis)
![Docker](https://img.shields.io/badge/Docker%20Compose-Ready-blue?logo=docker)

A production-grade, highly scalable distributed microservices platform designed for high-throughput URL shortening and real-time clickstream analytics. 

The architecture strictly decouples the latency-critical redirect path from analytics computation using **Apache Kafka** and the **Transactional Outbox Pattern**, guaranteeing sub-millisecond lookup latency, zero data loss, and protection against cascading system failures.

---

## 🏛 System Architecture

![System Architecture](assets/architecture.png)

---

## 🎯 Key Distributed Systems Patterns

### 1. Dual-Write Problem Solved: Transactional Outbox Pattern
* **Challenge:** Updating PostgreSQL and publishing an event to Kafka across separate networks risks state inconsistency if either step fails (e.g., database commits but network times out before Kafka acknowledges).
* **Implementation:** On each URL redirect, a `UrlClickEvent` is atomically persisted to a local `outbox_events` table within the same ACID database transaction. A background publisher polls the outbox table and streams events to Kafka with guaranteed **At-Least-Once Delivery**, marking them as processed upon broker ACK.

### 2. Duplicate Delivery Prevention: Idempotent Consumer
* **Challenge:** At-least-once message delivery, network retries, and Kafka consumer group rebalances inevitably produce duplicate events, which would corrupt click counts.
* **Implementation:** Every published event carries an immutable `eventId` (UUID). The Analytics Service records processed event IDs in a dedicated `processed_events` table within the same transaction as the metric update. Duplicate events are safely acknowledged and skipped without side effects.

### 3. Fault Isolation: Dead Letter Queue (DLQ / DLT)
* **Challenge:** Malformed event payloads or unhandled runtime exceptions (Poison Pills) can trigger infinite consumer retry loops, blocking the partition offset and stalling all subsequent event processing.
* **Implementation:** Configured a resilient Spring Kafka `DefaultErrorHandler` with exponential backoff. Messages exceeding the maximum retry threshold are automatically routed to `url-clicks.DLT` (Dead Letter Topic) for offline inspection and alerting.

### 4. Cascade Failure Prevention: Resilience4j Circuit Breaker & Fallback
* **Challenge:** Downtime, network partitions, or high latency in the Analytics Service must never degrade the API Gateway or impact the critical URL redirection path.
* **Implementation:** Analytics routes in Spring Cloud Gateway are wrapped with a **Resilience4j Circuit Breaker**. If failure rates exceed 50%, the circuit trips open, immediately routing requests to a fallback controller that returns a standardized **RFC 7807 Problem Detail (503 Service Unavailable)** response without exhausting gateway worker threads.

### 5. High-Performance Caching & Cache Penetration Defense
* **Challenge:** High read traffic and malicious key-probing attacks (querying non-existent short codes) can bypass the cache and overload PostgreSQL.
* **Implementation:** Employs the **Cache-Aside** pattern backed by Redis. Successful lookups are cached with a configurable TTL. Non-existent short codes are stored with a negative sentinel marker (`__NOT_FOUND__`) with a 5-minute TTL, eliminating database penetration attacks.

### 6. Distributed Rate Limiting
* **Implementation:** Spring Cloud Gateway integrates **Redis Token Bucket Rate Limiting** to protect endpoints against brute-force attacks and denial-of-service, enforcing separate rate limits for read redirects and write creation requests.

### 7. End-to-End Distributed Tracing
* **Implementation:** Fully instrumented with **Micrometer Tracing**, **Brave**, and **OpenZipkin**. Correlation IDs (`traceId` and `spanId`) propagate across HTTP requests, Gateway filters, and Kafka record headers, giving unified observability across asynchronous boundaries.

---

## 📦 Project Structure

A clean, modular Gradle monorepo adhering to domain separation:

```
url-shortener/
├── api-gateway/         # Spring Cloud Gateway (Routing, Redis Rate Limiting, Circuit Breaker)
├── contracts/           # Shared Java records, DTOs, and Kafka event definitions
├── url-service/         # Core domain service (Base62 hashing, Redis Cache-Aside, Transactional Outbox)
├── analytics-service/   # Async consumer (Idempotent processing, click metrics, DLQ error handler)
├── docker/              # Docker initialization SQL scripts and container configs
├── .github/workflows/   # CI/CD pipeline (Tests with Testcontainers & parallel Docker builds)
├── compose.yaml         # Production-grade multi-container orchestrator
└── settings.gradle.kts  # Gradle multi-module root definition
```

---

## 🚀 Quick Start (Docker Compose)

Spin up the entire microservice ecosystem, databases, message broker, and tracing tools with a single command:

```bash
# 1. Clone repository
git clone https://github.com/DmytroChup/url-shortener.git
cd url-shortener

# 2. Build and launch all services in detached mode
docker compose up --build -d
```

### Services & Port Mappings:
| Service               | Host Port    | Internal Port | Technology                   | Purpose                              |
|-----------------------|--------------|---------------|------------------------------|--------------------------------------|
| **API Gateway**       | `8080`       | `8080`        | Spring Cloud Gateway (Netty) | Public entry point & reverse proxy   |
| **URL Service**       | `8081`       | `8081`        | Spring Boot 4 / Java 25      | URL shortening & 302 redirects       |
| **Analytics Service** | `8082`       | `8082`        | Spring Boot 4 / Java 25      | Click analytics & Kafka consumer     |
| **OpenZipkin UI**     | `9411`       | `9411`        | Zipkin                       | Distributed tracing dashboard        |
| **PostgreSQL**        | `5444`       | `5432`        | PostgreSQL 16 Alpine         | Databases: `urls_db`, `analytics_db` |
| **Apache Kafka**      | `9092, 9094` | `9092`        | Apache Kafka 4.3 (KRaft)     | Event streaming broker               |
| **Redis**             | `6379`       | `6379`        | Redis 8.8 Alpine             | In-memory cache & rate limiter       |

---

## 🧪 Live Smoke Test (cURL)

Send requests directly to the **API Gateway** (`http://localhost:8080`):

### 1. Shorten a URL
```bash
curl -X POST http://localhost:8080/api/v1/urls/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://spring.io", "ttlDays": 30}'
```
**Response:**
```
"aB7xK9q"
```

### 2. Follow Short URL (HTTP 302 Redirect)
```bash
curl -i http://localhost:8080/aB7xK9q
```
**Response:**
```http
HTTP/1.1 302 Found
Location: https://spring.io
```
*(Notice: The redirect executes immediately in < 10ms while the click event is written to Outbox and processed asynchronously in Kafka).*

### 3. Retrieve Click Analytics
```bash
curl http://localhost:8080/api/v1/analytics/aB7xK9q
```
**Response:**
```json
{
  "shortCode": "aB7xK9q",
  "clickCount": 1
}
```

### 4. Verify Resilience4j Circuit Breaker Fallback
Stop the Analytics Service container to test system resilience:
```bash
docker compose stop analytics-service
curl -i http://localhost:8080/api/v1/analytics/aB7xK9q
```
**Fallback Response (RFC 7807):**
```http
HTTP/1.1 503 Service Unavailable
Content-Type: application/problem+json

{
  "type": "urn:problem-type:service-unavailable",
  "title": "Service Unavailable",
  "status": 503,
  "detail": "Analytics service is temporarily unavailable. Please try again later."
}
```

### 5. Inspect Traces in Zipkin
Open `http://localhost:9411/zipkin/` in your browser and click **"Run Query"** to view end-to-end trace latency spanning the Gateway, URL Service, Outbox publisher, and Kafka consumer execution.

---

## 🛠 Testing & CI/CD Pipeline

The project enforces strict automated testing discipline:
* **Unit Tests:** Fast, isolated domain logic testing using JUnit 5 and Mockito.
* **Integration Tests:** Comprehensive integration test suites using **Testcontainers** (real PostgreSQL, Redis, and Apache Kafka instances spun up in isolated Docker containers per test run).
* **CI/CD Pipeline:** Fully automated via **GitHub Actions** (`.github/workflows/ci.yml`):
  * Automated builds on **Java 25 (Eclipse Temurin)** with Gradle dependency caching.
  * Multi-module integration test execution with Docker-in-Docker Testcontainers.
  * Matrix-based parallel validation of all service **Dockerfiles**.
  * Automatic JUnit test report artifacts upload on test failure.

```bash
# Run the entire test suite locally across all modules
./gradlew test
```

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
