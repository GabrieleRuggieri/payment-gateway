package com.finance.payment.settlement.service;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Step saga di settlement: trasferisce i fondi catturati al merchant (API acquirer mockata).
 *
 * <p>Soglie di fallimento (demo):
 * <ul>
 *   <li>importo &gt; 9 999,99 — l'autorizzazione fallisce già upstream; il settlement non viene raggiunto</li>
 *   <li>importo &gt; 4 999,99 — il settlement fallisce → attiva la compensazione {@code SETTLEMENT_FAILED}
 *       (void capture + rimborso), esercitando l'intero percorso di compensazione</li>
 *   <li>importo ≤ 4 999,99 — settlement riuscito</li>
 * </ul>
 */
@Service
@Slf4j
public class SettlementService {

    private static final BigDecimal SETTLEMENT_FAIL_THRESHOLD = new BigDecimal("4999.99");

    /**
     * Regola i fondi catturati sul conto merchant.
     * Restituisce un esito negativo per importi superiori a {@value #SETTLEMENT_FAIL_THRESHOLD}
     * per esercitare il ramo di compensazione {@code SETTLEMENT_FAILED → PAYMENT_REFUNDED}.
     */
    public SettlementResult settle(UUID paymentId, UUID merchantId, BigDecimal amount, String currency) {
        log.info("Settling payment {} merchant {} amount {} {}", paymentId, merchantId, amount, currency);
        if (amount.compareTo(SETTLEMENT_FAIL_THRESHOLD) > 0) {
            log.warn("Settlement rejected for payment {} — amount {} exceeds mock threshold {}",
                    paymentId, amount, SETTLEMENT_FAIL_THRESHOLD);
            return SettlementResult.failure("Acquirer rejected: amount exceeds settlement limit");
        }
        return SettlementResult.success("SET-" + paymentId.toString().substring(0, 8).toUpperCase());
    }

    /**
     * Compensazione: rimborsa il cliente quando il settlement fallisce dopo una capture riuscita.
     *
     * @return riferimento rimborso per la pubblicazione dell'evento downstream
     */
    public RefundResult refund(UUID paymentId, BigDecimal amount, String currency) {
        log.info("Refunding payment {} amount {} {}", paymentId, amount, currency);
        return RefundResult.success("REF-" + paymentId.toString().substring(0, 8).toUpperCase());
    }

    /** Esito di un'operazione di settlement. */
    @Value
    @Builder
    public static class SettlementResult {
        boolean success;
        String settlementReference;
        String failureReason;

        static SettlementResult success(String reference) {
            return SettlementResult.builder().success(true).settlementReference(reference).build();
        }

        static SettlementResult failure(String reason) {
            return SettlementResult.builder().success(false).failureReason(reason).build();
        }
    }

    /** Esito di un'operazione di rimborso. */
    @Value
    @Builder
    public static class RefundResult {
        boolean success;
        String refundReference;
        String failureReason;

        static RefundResult success(String reference) {
            return RefundResult.builder().success(true).refundReference(reference).build();
        }
    }
}
