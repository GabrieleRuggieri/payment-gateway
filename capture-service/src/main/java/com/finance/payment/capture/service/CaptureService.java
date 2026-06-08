package com.finance.payment.capture.service;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class CaptureService {

    /**
     * TODO: Integrate with payment processor capture API (Resilience4j wrapped).
     */
    public CaptureResult capture(UUID paymentId, BigDecimal amount, String currency, String authorizationCode) {
        log.info("Capturing payment {} for {} {}", paymentId, amount, currency);
        // TODO: replace stub
        return CaptureResult.success("CAP-" + paymentId.toString().substring(0, 8).toUpperCase());
    }

    @Value
    @Builder
    public static class CaptureResult {
        boolean success;
        String captureReference;
        String failureReason;

        static CaptureResult success(String reference) {
            return CaptureResult.builder().success(true).captureReference(reference).build();
        }

        static CaptureResult failure(String reason) {
            return CaptureResult.builder().success(false).failureReason(reason).build();
        }
    }
}
