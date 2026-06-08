package com.finance.payment.authorization.service;

import com.finance.payment.authorization.client.ExternalProcessorClient;
import com.finance.payment.authorization.dto.AuthorizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Authorization saga step: reserve funds with the external processor and compensate on capture failure.
 * <p>
 * Idempotency is enforced at the Kafka consumer via {@link com.finance.payment.common.saga.SagaEventDedupService}
 * (PostgreSQL) — not Redis, to keep financial dedup durable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final ExternalProcessorClient processorClient;

    /**
     * Calls the processor to authorize (hold) the payment amount.
     */
    public AuthorizationResult authorize(UUID paymentId, BigDecimal amount, String currency) {
        return processorClient.authorize(paymentId, amount, currency);
    }

    /**
     * Saga compensation: void/release a prior authorization when capture fails downstream.
     */
    public void voidAuthorization(UUID paymentId, String authorizationCode) {
        log.info("Voiding authorization for payment {} code {}", paymentId, authorizationCode);
        processorClient.voidAuthorization(paymentId, authorizationCode);
    }
}
