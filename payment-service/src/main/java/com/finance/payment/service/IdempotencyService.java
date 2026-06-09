package com.finance.payment.service;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.api.dto.IdempotentResult;
import com.finance.payment.api.dto.PaymentResponse;
import com.finance.payment.domain.IdempotencyRecord;
import com.finance.payment.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Garantisce un solo avvio pagamento per chiave di idempotenza entro la finestra TTL configurata.
 * <p>
 * Persistenza: PostgreSQL ({@code idempotency_keys}) — coerente e durabile rispetto alle righe pagamento.
 * Redis non è usato qui: perdere una chiave di dedup potrebbe causare un addebito doppio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${payment.idempotency.ttl-hours:24}")
    private long ttlHours;

    /**
     * Esegue {@code operation} una sola volta per chiave nel contesto transazionale del chiamante.
     * Richieste concorrenti con la stessa chiave attendono su {@code SELECT FOR UPDATE}.
     */
    @Transactional
    public IdempotentResult<PaymentResponse> executeIdempotent(String key, Supplier<PaymentResponse> operation) {
        Optional<IdempotencyRecord> existing = repository.findByKeyWithLock(key);

        if (existing.isPresent()) {
            log.debug("Idempotency replay for key={}", key);
            PaymentResponse cached = deserialize(existing.get().getResponseBody(), PaymentResponse.class);
            return IdempotentResult.replay(cached);
        }

        PaymentResponse result = operation.get();

        try {
            repository.save(IdempotencyRecord.builder()
                    .key(key)
                    .paymentId(result.id())
                    .responseStatus(200)
                    .responseBody(serialize(result))
                    .expiresAt(Instant.now().plus(Duration.ofHours(ttlHours)))
                    .build());
        } catch (DataIntegrityViolationException race) {
            log.debug("Concurrent idempotency insert for key={}, returning stored response", key);
            IdempotencyRecord winner = repository.findByKeyWithLock(key)
                    .orElseThrow(() -> race);
            return IdempotentResult.replay(deserialize(winner.getResponseBody(), PaymentResponse.class));
        }

        return IdempotentResult.fresh(result);
    }

    private String serialize(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    private <T> T deserialize(String json, Class<T> type) {
        return objectMapper.readValue(json, type);
    }
}
