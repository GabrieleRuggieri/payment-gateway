package com.finance.payment.service;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.domain.IdempotencyRecord;
import com.finance.payment.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${payment.idempotency.ttl-hours:24}")
    private long ttlHours;

    /**
     * TODO: Implement atomic idempotency flow:
     * 1. Try pessimistic lock / INSERT ON CONFLICT on idempotency key
     * 2. If hit → deserialize cached response and return (mark replay in controller)
     * 3. If miss → run operation, persist payment_id + serialized response + expires_at
     * 4. Entire flow must run in ONE @Transactional boundary with payment + outbox writes
     */
    @Transactional
    public <T> T executeIdempotent(String key, Supplier<T> operation, Class<T> responseType) {
        Optional<IdempotencyRecord> existing = repository.findByKeyWithLock(key);

        if (existing.isPresent()) {
            log.debug("Idempotency hit for key: {}", key);
            // TODO: Return deserialized cached response instead of throwing
            return deserialize(existing.get().getResponseBody(), responseType);
        }

        T result = operation.get();

        // TODO: Set payment_id on IdempotencyRecord from PaymentResponse.id()
        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(key)
                .paymentId(extractPaymentId(result))
                .responseStatus(200)
                .responseBody(serialize(result))
                .expiresAt(Instant.now().plus(Duration.ofHours(ttlHours)))
                .build();
        repository.save(record);

        return result;
    }

    public boolean wasReplay(String key) {
        // TODO: Track replay flag per request (e.g. ThreadLocal or return wrapper type)
        return repository.findById(key).isPresent();
    }

    private java.util.UUID extractPaymentId(Object result) {
        if (result instanceof PaymentResponse response) {
            return response.id();
        }
        throw new IllegalStateException("TODO: map idempotency record to payment id from " + result.getClass());
    }

    private String serialize(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    private <T> T deserialize(String json, Class<T> type) {
        return objectMapper.readValue(json, type);
    }
}
