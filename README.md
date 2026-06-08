# Payment Gateway con Saga + Idempotency

> **Dominio:** Payments / Distributed Systems  
> **Stack:** Spring Boot 3.x · Kafka · PostgreSQL · Redis · Resilience4j · React · Docker

---

## Indice

1. [Teoria del dominio](#1-teoria-del-dominio)
2. [Problemi fondamentali da risolvere](#2-problemi-fondamentali-da-risolvere)
3. [Architettura del sistema](#3-architettura-del-sistema)
4. [Schema del database](#4-schema-del-database)
5. [Pattern implementativi core](#5-pattern-implementativi-core)
6. [Implementazione Spring Boot](#6-implementazione-spring-boot)
7. [Saga coreografica con Kafka](#7-saga-coreografica-con-kafka)
8. [Resilience: circuit breaker e retry](#8-resilience-circuit-breaker-e-retry)
9. [Testing](#9-testing)
10. [Docker Compose locale](#10-docker-compose-locale)
11. [Cosa dire al colloquio](#11-cosa-dire-al-colloquio)

---

## 1. Teoria del dominio

### Cos'è un Payment Gateway

Un payment gateway è il sistema che orchestra l'esecuzione di un pagamento tra più parti: il merchant, il processore di pagamento, la banca emittente e la banca acquirer. Il suo compito non è semplicemente "eseguire" il pagamento, ma garantire che il pagamento venga eseguito **esattamente una volta**, anche in presenza di crash, timeout e retry.

### Il lifecycle di un pagamento

```
INITIATED → AUTHORIZED → CAPTURED → SETTLED
                ↓              ↓
            FAILED         REFUNDED
```


| Stato        | Significato                                  |
| ------------ | -------------------------------------------- |
| `INITIATED`  | La richiesta è stata ricevuta e validata     |
| `AUTHORIZED` | I fondi sono stati bloccati sulla carta      |
| `CAPTURED`   | I fondi sono stati effettivamente addebitati |
| `SETTLED`    | I fondi sono stati trasferiti al merchant    |
| `FAILED`     | Un passo ha fallito, fondi mai mossi         |
| `REFUNDED`   | Il pagamento è stato stornato post-capture   |


### Perché è difficile

In un sistema distribuito, ogni passo del lifecycle è una chiamata a un servizio esterno che può:

- Andare in timeout senza risposta
- Fallire dopo aver eseguito l'operazione (crash post-commit)
- Rispondere con un errore temporaneo che si risolve al retry

Il problema classico: il client fa retry di una chiamata già andata a buon fine. Risultato: **double charge**.

---

## 2. Problemi fondamentali da risolvere

### 2.1 Double charge

Il bug più costoso nel finance. Accade quando:

1. Il client invia una richiesta di pagamento
2. Il server esegue il pagamento con successo
3. La risposta va persa (crash, timeout di rete)
4. Il client riprova
5. Il pagamento viene eseguito **due volte**

**Soluzione:** Idempotency key.

### 2.2 Dual write

Il problema del doppio aggiornamento. Accade quando:

1. Il service aggiorna il DB (pagamento eseguito)
2. Il service tenta di pubblicare l'evento su Kafka
3. Il service crasha prima di pubblicare
4. Il DB è aggiornato, ma l'evento non è mai stato emesso
5. Gli altri servizi non sapranno mai del pagamento

**Soluzione:** Outbox pattern.

### 2.3 Transazioni distribuite senza 2PC

Non possiamo usare transazioni distribuite (2PC) su più microservizi — troppo accoppiamento, troppi rischi di deadlock. Ma dobbiamo comunque garantire consistenza tra Authorization, Capture e Settlement.

**Soluzione:** Saga coreografica con compensating transactions.

---

## 3. Architettura del sistema

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT / MERCHANT                    │
└──────────────────────────┬──────────────────────────────────┘
                           │ POST /payments  (+ Idempotency-Key header)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    PAYMENT SERVICE                          │
│                                                             │
│  ┌─────────────┐   ┌──────────────┐   ┌─────────────────┐  │
│  │  REST API   │──▶│  PaymentSvc  │──▶│  Outbox Writer  │  │
│  └─────────────┘   └──────────────┘   └────────┬────────┘  │
│                           │                     │           │
│                    ┌──────▼──────┐       ┌──────▼──────┐   │
│                    │  PostgreSQL │       │   Outbox    │   │
│                    │  payments   │       │   table     │   │
│                    └─────────────┘       └──────┬──────┘   │
└──────────────────────────────────────────────────┼──────────┘
                                                   │
                           ┌───────────────────────▼──────┐
                           │    OUTBOX RELAY (scheduler)  │
                           └───────────────────────┬──────┘
                                                   │ publish
                                                   ▼
                           ┌──────────────────────────────┐
                           │          KAFKA               │
                           │  topic: payment.events       │
                           └──┬───────────────────┬───────┘
                              │                   │
               ┌──────────────▼──┐         ┌──────▼──────────────┐
               │ AUTHORIZATION   │         │  NOTIFICATION       │
               │ SERVICE         │         │  SERVICE            │
               └──────────────┬──┘         └─────────────────────┘
                              │ payment.authorized
                              ▼
               ┌──────────────────────────┐
               │    CAPTURE SERVICE       │
               └──────────────┬───────────┘
                              │ payment.captured
                              ▼
               ┌──────────────────────────┐
               │   SETTLEMENT SERVICE     │
               └──────────────────────────┘
```

### Componenti


| Componente            | Responsabilità                                           |
| --------------------- | -------------------------------------------------------- |
| Payment Service       | Ricezione, validazione, idempotency check, stato FSM     |
| Authorization Service | Blocca i fondi tramite processore esterno (mockato)      |
| Capture Service       | Addebita i fondi autorizzati                             |
| Settlement Service    | Trasferisce i fondi al merchant                          |
| Outbox Relay          | Legge la outbox table e pubblica su Kafka atomicamente   |
| Notification Service  | Invia webhook/email al merchant per ogni cambio di stato |


---

## 4. Schema del database

```sql
-- Migration: V1__create_payments_schema.sql

-- Tabella principale dei pagamenti (append-friendly, no hard delete)
CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL,
    merchant_id     UUID NOT NULL,
    amount          NUMERIC(19, 4) NOT NULL,       -- MAI float/double
    currency        CHAR(3) NOT NULL,              -- ISO 4217: EUR, USD
    status          VARCHAR(50) NOT NULL,
    description     TEXT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,     -- optimistic locking

    CONSTRAINT payments_amount_positive CHECK (amount > 0),
    CONSTRAINT payments_status_valid CHECK (
        status IN ('INITIATED','AUTHORIZED','CAPTURED','SETTLED','FAILED','REFUNDED')
    )
);

-- Indice unique su idempotency_key per garantire l'unicità
CREATE UNIQUE INDEX idx_payments_idempotency ON payments (idempotency_key);
CREATE INDEX idx_payments_merchant ON payments (merchant_id, created_at DESC);
CREATE INDEX idx_payments_status ON payments (status) WHERE status NOT IN ('SETTLED','FAILED');

-- Audit trail immutabile: ogni cambio di stato è una riga
CREATE TABLE payment_events (
    id              BIGSERIAL PRIMARY KEY,
    payment_id      UUID NOT NULL REFERENCES payments(id),
    event_type      VARCHAR(100) NOT NULL,
    old_status      VARCHAR(50),
    new_status      VARCHAR(50),
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255)   -- service name che ha generato l'evento
);

CREATE INDEX idx_payment_events_payment ON payment_events (payment_id, created_at);

-- Outbox table: scrittura atomica con il business data
CREATE TABLE payment_outbox (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_id    UUID NOT NULL,         -- payment_id
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    topic           VARCHAR(255) NOT NULL,
    partition_key   VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,

    CONSTRAINT outbox_status_valid CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','FAILED'))
);

-- Solo i PENDING interessano al relay — partial index molto efficiente
CREATE INDEX idx_outbox_pending ON payment_outbox (created_at) WHERE status = 'PENDING';

-- Idempotency cache: evita di riprocessare richieste già viste
CREATE TABLE idempotency_keys (
    key             VARCHAR(255) PRIMARY KEY,
    payment_id      UUID NOT NULL,
    response_status INT NOT NULL,
    response_body   JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
```

---

## 5. Pattern implementativi core

### 5.1 Idempotency Key

L'idempotency key è un identificatore univoco fornito dal client per ogni richiesta. Se la stessa key viene inviata due volte, il server restituisce la risposta originale senza rieseguire l'operazione.

**Regole:**

- Generata dal client, non dal server (il server non sa se è un retry)
- UUID v4 o stringa opaca di almeno 32 caratteri
- Scadenza dopo 24 ore (finestra di retry ragionevole)
- Salvata prima di eseguire qualsiasi operazione

**Flusso:**

```
1. Ricevi richiesta con header Idempotency-Key: <key>
2. Cerca la key nel DB
3a. Se trovata → restituisci la risposta cached (HTTP 200 con header Idempotent-Replayed: true)
3b. Se non trovata → esegui operazione, salva key + risposta, restituisci risultato
```

**Attenzione:** il check e il salvataggio devono essere nella stessa transazione, altrimenti due richieste concorrenti con la stessa key possono passare entrambe il check. Usare `INSERT ... ON CONFLICT DO NOTHING` con controllo delle righe inserite.

### 5.2 Outbox Pattern

Il problema del dual write si risolve scrivendo nello stesso DB transaction sia il business update sia il messaggio da pubblicare. Un processo separato (Outbox Relay) legge poi la tabella outbox e pubblica su Kafka.

**Garanzie:**

- Il messaggio viene pubblicato *se e solo se* la transazione DB va a buon fine
- At-least-once delivery (il relay può pubblicare lo stesso messaggio più volte in caso di crash — i consumer devono essere idempotenti)
- Ordinamento garantito per `aggregate_id` se si usa una singola partizione Kafka per payment

**Outbox Relay con SELECT FOR UPDATE SKIP LOCKED:**

```sql
SELECT * FROM payment_outbox
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

`SKIP LOCKED` permette a più istanze del relay di girare in parallelo senza bloccarsi a vicenda.

### 5.3 Saga Coreografica

Nella Saga coreografica non c'è un orchestratore centrale. Ogni servizio ascolta eventi e reagisce pubblicando il prossimo evento. In caso di fallimento, pubblica un evento di compensazione che annulla i passi precedenti.

```
PaymentInitiated
    → AuthorizationService ascolta → esegue autorizzazione
        → PaymentAuthorized (successo)
        → AuthorizationFailed (fallimento) ← compensation: nessuna (nulla da annullare)

PaymentAuthorized
    → CaptureService ascolta → esegue capture
        → PaymentCaptured (successo)
        → CaptureFailed → compensation: rilascia autorizzazione (void)

PaymentCaptured
    → SettlementService ascolta → esegue settlement
        → PaymentSettled (successo)
        → SettlementFailed → compensation: emetti rimborso (refund)
```

---

## 6. Implementazione Spring Boot

### 6.1 Struttura del progetto

```
payment-gateway/
├── payment-service/
│   ├── src/main/java/com/finance/payment/
│   │   ├── api/
│   │   │   ├── PaymentController.java
│   │   │   ├── dto/
│   │   │   │   ├── CreatePaymentRequest.java
│   │   │   │   └── PaymentResponse.java
│   │   │   └── filter/IdempotencyFilter.java
│   │   ├── domain/
│   │   │   ├── Payment.java
│   │   │   ├── PaymentEvent.java
│   │   │   ├── PaymentOutbox.java
│   │   │   └── PaymentStatus.java
│   │   ├── repository/
│   │   │   ├── PaymentRepository.java
│   │   │   ├── PaymentOutboxRepository.java
│   │   │   └── IdempotencyKeyRepository.java
│   │   ├── service/
│   │   │   ├── PaymentService.java
│   │   │   ├── IdempotencyService.java
│   │   │   └── OutboxRelayService.java
│   │   └── config/
│   │       ├── KafkaConfig.java
│   │       └── ResilienceConfig.java
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│           └── V1__create_payments_schema.sql
├── authorization-service/
├── capture-service/
├── settlement-service/
└── docker-compose.yml
```

### 6.2 Domain entities

```java
// Payment.java
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    // CRITICO: BigDecimal, mai double o float per importi monetari
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // Optimistic locking: previene aggiornamenti concorrenti
    @Version
    private Long version;

    // Factory method: i costruttori pubblici sono vietati — invarianti sempre garantite
    public static Payment initiate(
            String idempotencyKey,
            UUID merchantId,
            BigDecimal amount,
            String currency) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!Currency.getAvailableCurrencies()
                .stream().map(Currency::getCurrencyCode)
                .anyMatch(c -> c.equals(currency))) {
            throw new IllegalArgumentException("Invalid currency: " + currency);
        }

        Payment p = new Payment();
        p.idempotencyKey = idempotencyKey;
        p.merchantId = merchantId;
        p.amount = amount.setScale(4, RoundingMode.UNNECESSARY);
        p.currency = currency;
        p.status = PaymentStatus.INITIATED;
        return p;
    }

    // Transizioni di stato esplicite — nessun setter pubblico sullo status
    public void authorize() {
        assertStatus(PaymentStatus.INITIATED);
        this.status = PaymentStatus.AUTHORIZED;
    }

    public void capture() {
        assertStatus(PaymentStatus.AUTHORIZED);
        this.status = PaymentStatus.CAPTURED;
    }

    public void settle() {
        assertStatus(PaymentStatus.CAPTURED);
        this.status = PaymentStatus.SETTLED;
    }

    public void fail(PaymentStatus fromStatus) {
        assertStatus(fromStatus);
        this.status = PaymentStatus.FAILED;
    }

    private void assertStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                "Invalid transition: " + this.status + " → cannot proceed from " + expected
            );
        }
    }
}
```

```java
// PaymentOutbox.java
@Entity
@Table(name = "payment_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key")
    private String partitionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    private int attempts = 0;

    @Column(name = "last_error")
    private String lastError;

    @CreationTimestamp
    private Instant createdAt;

    private Instant processedAt;

    public static PaymentOutbox of(UUID paymentId, String eventType,
                                    Map<String, Object> payload, String topic) {
        PaymentOutbox o = new PaymentOutbox();
        o.aggregateId = paymentId;
        o.eventType = eventType;
        o.payload = payload;
        o.topic = topic;
        o.partitionKey = paymentId.toString(); // stesso payment → stessa partizione → ordinamento garantito
        return o;
    }

    public void markProcessing() { this.status = OutboxStatus.PROCESSING; this.attempts++; }
    public void markPublished() { this.status = OutboxStatus.PUBLISHED; this.processedAt = Instant.now(); }
    public void markFailed(String error) { this.status = OutboxStatus.FAILED; this.lastError = error; }
}
```

### 6.3 Payment Service

```java
// PaymentService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request, String idempotencyKey) {

        // 1. Idempotency check — dentro la stessa transazione
        return idempotencyService.executeIdempotent(idempotencyKey, () -> {

            // 2. Crea il pagamento
            Payment payment = Payment.initiate(
                idempotencyKey,
                request.merchantId(),
                request.amount(),
                request.currency()
            );
            paymentRepository.save(payment);

            // 3. Scrivi nella outbox — STESSA TRANSAZIONE del save
            // Se il DB commit fallisce, né payment né outbox vengono salvati
            PaymentOutbox outboxEvent = PaymentOutbox.of(
                payment.getId(),
                "PaymentInitiated",
                Map.of(
                    "paymentId", payment.getId().toString(),
                    "amount", payment.getAmount(),
                    "currency", payment.getCurrency(),
                    "merchantId", payment.getMerchantId().toString()
                ),
                "payment.events"
            );
            outboxRepository.save(outboxEvent);

            log.info("Payment initiated: id={}, idempotencyKey={}", payment.getId(), idempotencyKey);

            return PaymentResponse.from(payment);
        });
    }

    @Transactional
    public void handleAuthorized(UUID paymentId) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.authorize();

        outboxRepository.save(PaymentOutbox.of(
            paymentId, "PaymentAuthorized",
            Map.of("paymentId", paymentId.toString()),
            "payment.events"
        ));
    }

    @Transactional
    public void handleAuthorizationFailed(UUID paymentId, String reason) {
        Payment payment = findPaymentOrThrow(paymentId);
        payment.fail(PaymentStatus.INITIATED);

        log.warn("Authorization failed for payment {}: {}", paymentId, reason);
        // Nessuna compensazione necessaria — nessun fondo era stato spostato
    }

    private Payment findPaymentOrThrow(UUID id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
```

### 6.4 Idempotency Service

```java
// IdempotencyService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public <T> T executeIdempotent(String key, Supplier<T> operation) {
        // Tenta INSERT — se la key esiste già, la query non inserisce nulla
        // Usiamo un approccio pessimistico: lock sulla riga per concorrenza
        Optional<IdempotencyRecord> existing = repository.findByKeyWithLock(key);

        if (existing.isPresent()) {
            log.debug("Idempotency hit for key: {}", key);
            // Restituisce la risposta precedentemente serializzata
            return deserialize(existing.get().getResponseBody());
        }

        // Esegui l'operazione
        T result = operation.get();

        // Salva il risultato — se fallisce, la transazione intera fa rollback
        IdempotencyRecord record = IdempotencyRecord.builder()
            .key(key)
            .responseBody(serialize(result))
            .responseStatus(200)
            .expiresAt(Instant.now().plus(Duration.ofHours(24)))
            .build();
        repository.save(record);

        return result;
    }

    private String serialize(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(String json) {
        try { return (T) objectMapper.readValue(json, Object.class); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
}
```

### 6.5 Outbox Relay Service

```java
// OutboxRelayService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Esegue ogni secondo — in produzione usare Debezium CDC per maggiore efficienza
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void relay() {
        // SELECT FOR UPDATE SKIP LOCKED — safe per istanze multiple
        List<PaymentOutbox> pending = outboxRepository.findPendingWithLock(100);

        if (pending.isEmpty()) return;

        log.debug("Relaying {} outbox events", pending.size());

        for (PaymentOutbox event : pending) {
            try {
                event.markProcessing();

                String payload = objectMapper.writeValueAsString(event.getPayload());

                // Invio sincrono con timeout — in produzione valutare invio asincrono
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                    .get(5, TimeUnit.SECONDS);

                event.markPublished();

            } catch (Exception e) {
                log.error("Failed to relay outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed(e.getMessage());
                // Non bloccare gli altri eventi — continua il loop
            }
        }
    }
}
```

```java
// PaymentOutboxRepository.java
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    @Query("""
        SELECT o FROM PaymentOutbox o
        WHERE o.status = 'PENDING'
        ORDER BY o.createdAt ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """)
    List<PaymentOutbox> findPendingWithLock(@Param("limit") int limit);
}
```

---

## 7. Saga coreografica con Kafka

### 7.1 Kafka Config

```java
// KafkaConfig.java
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment.events")
            .partitions(6)        // partizioni basate su merchant_id per ordinamento
            .replicas(1)          // 1 in locale, 3 in produzione
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
            .build();
    }

    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaProperties props) {
        Map<String, Object> config = new HashMap<>(props.buildProducerProperties(null));
        // Idempotent producer: garantisce exactly-once delivery lato Kafka
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

### 7.2 Authorization Service Consumer

```java
// AuthorizationEventConsumer.java
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationEventConsumer {

    private final AuthorizationService authorizationService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "payment.events",
        groupId = "authorization-service",
        containerFactory = "paymentEventListenerFactory"
    )
    public void handlePaymentEvent(ConsumerRecord<String, String> record) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);

            switch (event.getEventType()) {
                case "PaymentInitiated" -> handleInitiated(event);
                // Ignora tutti gli altri tipi di evento
                default -> log.trace("Ignoring event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing event from partition={} offset={}: {}",
                record.partition(), record.offset(), e.getMessage());
            // Non rilanciare: l'evento va nella dead letter queue
            // In produzione configurare DeadLetterPublishingRecoverer
        }
    }

    private void handleInitiated(PaymentEvent event) {
        UUID paymentId = UUID.fromString(event.getPayload().get("paymentId").toString());

        try {
            // Chiama il processore esterno (mockato in locale)
            AuthorizationResult result = authorizationService.authorize(
                paymentId,
                new BigDecimal(event.getPayload().get("amount").toString()),
                event.getPayload().get("currency").toString()
            );

            String resultEvent = result.isSuccess() ? "PaymentAuthorized" : "AuthorizationFailed";
            Map<String, Object> payload = new HashMap<>(event.getPayload());
            payload.put("authorizationCode", result.getAuthorizationCode());

            kafkaTemplate.send("payment.events", paymentId.toString(),
                objectMapper.writeValueAsString(new PaymentEvent(resultEvent, payload)));

        } catch (Exception e) {
            log.error("Authorization error for payment {}: {}", paymentId, e.getMessage());
            // Pubblica evento di fallimento per attivare la compensazione
            publishAuthorizationFailed(paymentId, e.getMessage());
        }
    }
}
```

---

## 8. Resilience: circuit breaker e retry

```java
// ResilienceConfig.java
@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerConfig processorCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)              // apri se 50% delle chiamate falliscono
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();
    }

    @Bean
    public RetryConfig processorRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2))  // 500ms, 1s, 2s
            // Jitter per evitare thundering herd
            .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(500, 2, 0.3))
            .retryOnException(e -> e instanceof TemporaryProcessorException)
            .build();
    }
}
```

```java
// ExternalProcessorClient.java
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalProcessorClient {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RestTemplate restTemplate;

    public AuthorizationResult authorize(UUID paymentId, BigDecimal amount, String currency) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment-processor");
        Retry retry = retryRegistry.retry("payment-processor");

        // Composizione: prima retry, poi circuit breaker
        Supplier<AuthorizationResult> decorated = Decorators
            .ofSupplier(() -> callProcessor(paymentId, amount, currency))
            .withCircuitBreaker(cb)
            .withRetry(retry)
            .decorate();

        return Try.ofSupplier(decorated)
            .getOrElseThrow(e -> new ProcessorUnavailableException(
                "Processor unavailable after retries: " + e.getMessage()
            ));
    }

    private AuthorizationResult callProcessor(UUID paymentId, BigDecimal amount, String currency) {
        // In locale: mock sempre success tranne per amount > 9999
        if (amount.compareTo(new BigDecimal("9999")) > 0) {
            throw new TemporaryProcessorException("Limit exceeded");
        }
        return AuthorizationResult.success("AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
```

---

## 9. Testing

### 9.1 Unit test del domain

```java
// PaymentTest.java
class PaymentTest {

    @Test
    void shouldTransitionFromInitiatedToAuthorized() {
        Payment payment = Payment.initiate(
            "key-123", UUID.randomUUID(),
            new BigDecimal("100.00"), "EUR"
        );

        payment.authorize();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Payment payment = Payment.initiate(
            "key-123", UUID.randomUUID(),
            new BigDecimal("100.00"), "EUR"
        );

        // Non si può fare capture da INITIATED, ci vuole prima AUTHORIZED
        assertThatThrownBy(payment::capture)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid transition");
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Payment.initiate(
            "key-123", UUID.randomUUID(),
            new BigDecimal("-50.00"), "EUR"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldStoreFourDecimalPlaces() {
        Payment payment = Payment.initiate(
            "key-123", UUID.randomUUID(),
            new BigDecimal("99.9"), "EUR"
        );
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("99.9000"));
    }
}
```

### 9.2 Integration test con Testcontainers

```java
// PaymentServiceIntegrationTest.java
@SpringBootTest
@Testcontainers
@Transactional
class PaymentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("payments_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired PaymentService paymentService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired PaymentOutboxRepository outboxRepository;

    @Test
    void shouldCreatePaymentAndOutboxEventInSameTransaction() {
        var request = new CreatePaymentRequest(UUID.randomUUID(), new BigDecimal("150.00"), "EUR", null);
        String idempotencyKey = UUID.randomUUID().toString();

        PaymentResponse response = paymentService.initiatePayment(request, idempotencyKey);

        // Verifica payment salvato
        Payment saved = paymentRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(saved.getAmount()).isEqualByComparingTo("150.0000");

        // Verifica outbox event creato nella stessa transazione
        List<PaymentOutbox> outboxEvents = outboxRepository.findByAggregateId(response.id());
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PaymentInitiated");
        assertThat(outboxEvents.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void shouldReturnSameResponseForDuplicateIdempotencyKey() {
        var request = new CreatePaymentRequest(UUID.randomUUID(), new BigDecimal("75.00"), "EUR", null);
        String idempotencyKey = UUID.randomUUID().toString();

        PaymentResponse first = paymentService.initiatePayment(request, idempotencyKey);
        PaymentResponse second = paymentService.initiatePayment(request, idempotencyKey);

        // Stessa risposta, stesso payment ID — nessun double-charge
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(paymentRepository.count()).isEqualTo(1L);
    }
}
```

---

## 10. Docker Compose locale

```yaml
# docker-compose.yml
version: '3.9'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: payments
      POSTGRES_USER: payments_user
      POSTGRES_PASSWORD: payments_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U payments_user -d payments"]
      interval: 5s
      timeout: 5s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

  payment-service:
    build: ./payment-service
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_started
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payments
      SPRING_DATASOURCE_USERNAME: payments_user
      SPRING_DATASOURCE_PASSWORD: payments_pass
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  authorization-service:
    build: ./authorization-service
    ports:
      - "8081:8080"
    depends_on:
      - kafka
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  capture-service:
    build: ./capture-service
    ports:
      - "8082:8080"
    depends_on:
      - kafka
      - postgres
    environment:
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payments

volumes:
  postgres_data:
```

### Comandi per avviare e testare

```bash
# Avvia tutto
docker compose up -d

# Monitora i log del payment service
docker compose logs -f payment-service

# Crea un pagamento
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-$(uuidgen)" \
  -d '{
    "merchantId": "550e8400-e29b-41d4-a716-446655440000",
    "amount": "99.99",
    "currency": "EUR"
  }'

# Retry con la stessa key — deve restituire la stessa risposta
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-STESSO-VALORE" \
  -d '{ "merchantId": "550e8400-e29b-41d4-a716-446655440000", "amount": "99.99", "currency": "EUR" }'

# Verifica outbox events
docker compose exec postgres psql -U payments_user -d payments \
  -c "SELECT id, event_type, status, attempts FROM payment_outbox ORDER BY created_at DESC LIMIT 10;"

# Visualizza Kafka UI
open http://localhost:8090
```

---

## 11. Domande

### Domanda: "Come eviti il double charge?"

> *"Uso un'idempotency key: il client genera un UUID per ogni richiesta e lo manda nell'header. Il server, nella stessa transazione in cui salva il pagamento, registra la key con la risposta. Se arriva la stessa key una seconda volta, restituisce la risposta salvata senza rieseguire nulla. Il check e il salvataggio devono essere atomici — non basta un SELECT prima dell'INSERT, due richieste concorrenti passerebbero entrambe. Uso INSERT con ON CONFLICT DO NOTHING e controllo le righe inserite."*

### Domanda: "Cos'è l'Outbox pattern e perché lo usi?"

> *"Il problema del dual write: se aggiorno il DB e poi pubblico su Kafka in due operazioni separate, il servizio può crashare nel mezzo. Il DB è aggiornato ma l'evento non è mai pubblicato — gli altri servizi non sapranno mai del pagamento. Con l'Outbox scrivo il messaggio da pubblicare nella stessa transazione DB del business update. Un processo separato legge la tabella outbox e pubblica su Kafka. Se crasha, rilegge. Garanzia: at-least-once delivery, quindi i consumer devono essere idempotenti — ma almeno nessun evento si perde."*

### Domanda: "Perché la Saga coreografica e non l'orchestrazione?"

> *"Con l'orchestratore hai un singolo punto di fallimento e un servizio che conosce il flow completo — accoppiamento alto. Con la coreografia ogni servizio è autonomo e reagisce a eventi. Il prezzo è che il flow è implicito — devi leggere tutti i consumer per capire cosa succede. Per un flow semplice come authorize → capture → settle, la coreografia è più semplice. Per flow complessi con molte diramazioni, l'orchestratore diventa più leggibile."*

### Domanda: "Come gestisci i fallimenti?"

> *"Ogni passo della saga pubblica un evento di successo o di fallimento. Un fallimento trigger una compensating transaction — per esempio, se il capture fallisce dopo l'autorizzazione, pubblichiamo un evento che dice all'authorization service di fare un void. Non rollback distribuito, ma forward correction: il sistema procede sempre in avanti, annullando i passi precedenti con operazioni esplicite."*

