package com.finance.payment.authorization.service;

import com.finance.payment.authorization.client.ExternalProcessorClient;
import com.finance.payment.authorization.dto.AuthorizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Step saga di autorizzazione: riserva fondi sul processore esterno e compensa in caso di fallimento capture.
 * <p>
 * L'idempotenza è garantita dal consumer Kafka via {@link com.finance.payment.common.saga.SagaEventDedupService}
 * (PostgreSQL) — non Redis, per mantenere la dedup finanziaria durabile.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final ExternalProcessorClient processorClient;

    /**
     * Invoca il processore per autorizzare (bloccare) l'importo del pagamento.
     */
    public AuthorizationResult authorize(UUID paymentId, BigDecimal amount, String currency) {
        return processorClient.authorize(paymentId, amount, currency);
    }

    /**
     * Compensazione saga: annulla/rilascia un'autorizzazione precedente quando la capture fallisce downstream.
     */
    public void voidAuthorization(UUID paymentId, String authorizationCode) {
        log.info("Voiding authorization for payment {} code {}", paymentId, authorizationCode);
        processorClient.voidAuthorization(paymentId, authorizationCode);
    }
}
