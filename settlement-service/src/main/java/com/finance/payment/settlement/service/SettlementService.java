package com.finance.payment.settlement.service;

import com.finance.payment.common.processor.PaymentProcessor;
import com.finance.payment.common.processor.ProcessorResult;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Step saga di settlement: trasferisce / conferma i fondi al merchant (mock o verifica Stripe).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final PaymentProcessor paymentProcessor;

    /**
     * Regola i fondi catturati sul conto merchant.
     *
     * @param captureReference reference della capture (con Stripe è tipicamente il PaymentIntent id {@code pi_…})
     */
    public SettlementResult settle(
            UUID paymentId, UUID merchantId, BigDecimal amount, String currency, String captureReference) {
        log.info("Settling payment {} merchant {} amount {} {}", paymentId, merchantId, amount, currency);
        ProcessorResult result = paymentProcessor.settle(paymentId, merchantId, amount, currency, captureReference);
        if (!result.success()) {
            return SettlementResult.failure(result.failureReason());
        }
        return SettlementResult.success(result.reference());
    }

    /**
     * Compensazione: rimborsa il cliente quando il settlement fallisce dopo una capture riuscita.
     *
     * @param captureReference PaymentIntent o charge da rimborsare
     */
    public RefundResult refund(UUID paymentId, BigDecimal amount, String currency, String captureReference) {
        log.info("Refunding payment {} amount {} {}", paymentId, amount, currency);
        ProcessorResult result = paymentProcessor.refund(paymentId, amount, currency, captureReference);
        if (!result.success()) {
            throw new IllegalStateException("Refund failed: " + result.failureReason());
        }
        return RefundResult.success(result.reference());
    }

    /** Esito settlement. */
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

    /** Esito rimborso. */
    @Value
    @Builder
    public static class RefundResult {
        String refundReference;

        static RefundResult success(String reference) {
            return RefundResult.builder().refundReference(reference).build();
        }
    }
}
