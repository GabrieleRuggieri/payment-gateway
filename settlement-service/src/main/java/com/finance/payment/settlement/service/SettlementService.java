package com.finance.payment.settlement.service;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class SettlementService {

    /**
     * TODO: Transfer funds to merchant account via acquirer/banking API.
     */
    public SettlementResult settle(UUID paymentId, UUID merchantId, BigDecimal amount, String currency) {
        log.info("Settling payment {} merchant {} amount {} {}", paymentId, merchantId, amount, currency);
        return SettlementResult.success("SET-" + paymentId.toString().substring(0, 8).toUpperCase());
    }

    /**
     * TODO: Compensation — issue refund when settlement fails after capture.
     */
    public void refund(UUID paymentId, BigDecimal amount, String currency) {
        throw new UnsupportedOperationException("TODO: call refund API and publish PaymentRefunded");
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
}
