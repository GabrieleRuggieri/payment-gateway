package com.finance.payment.settlement.service;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Settlement saga step: transfer captured funds to the merchant (mocked acquirer API).
 *
 * <p>Failure thresholds (demo):
 * <ul>
 *   <li>amount &gt; 9 999.99 — authorization already fails upstream; settlement never reached</li>
 *   <li>amount &gt; 4 999.99 — settlement fails → triggers {@code SETTLEMENT_FAILED} compensation
 *       (capture void + refund), exercising the full compensation path</li>
 *   <li>amount ≤ 4 999.99 — settlement succeeds</li>
 * </ul>
 */
@Service
@Slf4j
public class SettlementService {

    private static final BigDecimal SETTLEMENT_FAIL_THRESHOLD = new BigDecimal("4999.99");

    /**
     * Settles captured funds to the merchant account.
     * Returns a failure result for amounts above {@value #SETTLEMENT_FAIL_THRESHOLD} to exercise
     * the {@code SETTLEMENT_FAILED → PAYMENT_REFUNDED} compensation branch.
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
     * Compensation: refund the customer when settlement fails after a successful capture.
     *
     * @return refund reference for downstream event publishing
     */
    public RefundResult refund(UUID paymentId, BigDecimal amount, String currency) {
        log.info("Refunding payment {} amount {} {}", paymentId, amount, currency);
        return RefundResult.success("REF-" + paymentId.toString().substring(0, 8).toUpperCase());
    }

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
