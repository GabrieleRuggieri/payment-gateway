# Gamma AI — Slide Deck Prompt

Usa questo prompt direttamente nella barra di Gamma ("Create with AI") per generare una presentazione del progetto.

---

## Prompt

```
Create a professional presentation for a distributed payment gateway system built with Java 21 / Spring Boot 4, Apache Kafka KRaft, PostgreSQL 16, Redis 7, Resilience4j, React 19 and Docker Compose.

--- DESIGN ---
Apply a warm light aesthetic throughout the entire deck:
- Background: off-white / cream (#fcfcf9), with card surfaces in warm cream (#f3f0ea) and soft peach accents (#fcd5c5)
- Accent color: vivid orange (#f26207) — use it for headlines, key labels, icons, and CTA elements
- Typography: Inter (sans-serif), clean and modern
- Slide radius: very rounded card containers (32px corners)
- Code blocks: dark surface (#1c1c1c) with warm orange syntax highlights
- No dark backgrounds on regular slides; only code snippet slides may use a dark surface
- Minimal, generous whitespace; bento-grid style card layout where possible
- Icons and diagrams: thin-line wireframe style on cream backgrounds

--- SLIDES (one per section) ---

Slide 1 — Title
"Payment Gateway — Saga, Outbox & Idempotency"
Subtitle: Distributed payments with exactly-once guarantees
Tech badges: Spring Boot 4 · Kafka KRaft · PostgreSQL 16 · Redis 7 · React 19 · Docker

Slide 2 — The problem
Three core distributed-systems challenges:
1. Double charge — client retries a payment already executed
2. Dual write — DB updated but Kafka event never published after a crash
3. Distributed transactions — coordinating Authorization → Capture → Settlement without 2PC
Each problem paired with its solution (Idempotency key / Outbox pattern / Choreographed Saga)

Slide 3 — Payment lifecycle
State machine flow diagram:
INITIATED → AUTHORIZED → CAPTURED → SETTLED
                ↓                ↓
             FAILED          REFUNDED
Include a small table: state name | what it means

Slide 4 — System architecture
Microservices diagram:
payment-ui → payment-service → [PostgreSQL, payment_outbox]
OutboxRelay → Kafka (payment.events)
Kafka → authorization-service → capture-service → settlement-service
Kafka → notification-service (webhook)
Kafka → payment-service-saga (aggregate update)
Highlight: single Kafka topic, choreography not orchestration

Slide 5 — Idempotency pattern
Exactly-once payment creation:
- Client sends UUID Idempotency-Key header
- Server does SELECT FOR UPDATE on idempotency_keys table
- If new: INSERT payment + INSERT outbox event + INSERT idempotency record — all in one transaction
- If duplicate: return saved response, set Idempotent-Replayed: true header
- PostgreSQL chosen over Redis for durability (money-critical path)
Show a simple sequence diagram

Slide 6 — Outbox pattern
Reliable event publishing:
- PaymentService writes payment row + outbox row in the same DB transaction
- OutboxRelayService polls every 1 s with SELECT FOR UPDATE SKIP LOCKED
- Publishes to Kafka, then marks outbox row as PROCESSED
- Guarantees at-least-once delivery even if service crashes between DB write and Kafka publish
Show a simple before/after timeline: "without outbox" (lost event) vs "with outbox" (guaranteed)

Slide 7 — Choreographed Saga
Event flow across services:
PaymentInitiated → authorization-service → PaymentAuthorized / AuthorizationFailed
PaymentAuthorized → capture-service → PaymentCaptured / CaptureFailed
PaymentCaptured → settlement-service → PaymentSettled / SettlementFailed → PaymentRefunded
Compensation paths highlighted in orange:
CaptureFailed → authorization-service voidAuthorization
SettlementFailed → settlement-service refund → PaymentRefunded

Slide 8 — Saga dedup (atomic)
Critical correctness detail:
- SagaEventDedupService uses PostgreSQL INSERT ON CONFLICT DO NOTHING
- Propagation.MANDATORY: dedup INSERT shares the consumer's transaction
- If processing fails → rollback includes dedup row → Kafka retries correctly
- Without this: dedup commits first → retry sees duplicate → event silently goes to DLT
Show a two-column comparison: "wrong pattern" vs "correct pattern"

Slide 9 — Resilience
Circuit breaker & retry on authorization:
- ExternalProcessorClient wrapped with Resilience4j
- Circuit breaker: opens at 50% failure rate, 30 s wait, sliding window 10
- Retry: 3 attempts, 500 ms exponential backoff
- Mock threshold: amount > 9 999 triggers TemporaryProcessorException
- DLT (payment.events.DLT): DefaultErrorHandler, 3 retries at 1 s, then dead-letter

Slide 10 — Infrastructure
Bento-style cards for each infrastructure component:
- PostgreSQL 16: shared DB, Flyway migrations, optimistic locking (version column)
- Kafka KRaft 7.8: no Zookeeper, single topic, 6 partitions, partition key = paymentId
- Redis 7: notification dedup (SET NX), webhook delivery guarantee
- Docker Compose: 13 services, automated image builds via one-shot build-backend/build-frontend containers, healthchecks + startup ordering

Slide 11 — Testing
Test pyramid:
- Unit tests (34): CaptureService, SettlementService, AuthorizationService, SagaEventDedupService, all consumer routing + dedup + error paths
- Integration tests: Testcontainers (PostgreSQL 16 + ConfluentKafkaContainer 7.8), payment creation + outbox co-commit, idempotency replay
- CI: GitHub Actions — backend job (Java 21, PostgreSQL service), frontend job (Node 22, tsc + Vite build), Docker Compose smoke test on push

Slide 12 — Demo UI
React 19 / Vite frontend at localhost:3000:
- Hero composer: enter amount + currency → create payment
- Configure freely card: set Merchant ID and Idempotency Key, retry same key to verify exactly-once
- Move faster card: live saga flow art — see each step highlight in real time as Kafka events flow
- API test collection: built-in Postman-style suite covering success, idempotency, validation errors, processor failure
- Nginx reverse proxy to payment-service at /api

Slide 13 — Key talking points (interview-ready)
Four punchy answers:
"How do you prevent double charge?" → Idempotency key: SELECT FOR UPDATE + INSERT in one transaction
"Why Outbox pattern?" → Dual write problem: atomic DB write guarantees at-least-once Kafka delivery
"Saga choreography vs orchestration?" → Choreography = no single point of failure; each service is autonomous
"How do you handle failures?" → Compensating transactions: always move forward, reverse previous steps explicitly

Slide 14 — Closing
"What payment will you process?"
Show the tech stack badges again with short one-line descriptions
GitHub link placeholder
```

---

## Design reference (copy-paste into Gamma Theme editor)

| Token | Value |
|---|---|
| Background | `#fcfcf9` |
| Card surface | `#f3f0ea` |
| Peach accent | `#fcd5c5` |
| Primary accent | `#f26207` |
| Text | `#1c1c1c` |
| Muted text | `#646464` |
| Code background | `#1c1c1c` |
| Font | Inter |
| Border radius | 32px (cards), 14px (chips) |
