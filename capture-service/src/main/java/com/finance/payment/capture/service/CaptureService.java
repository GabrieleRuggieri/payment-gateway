package com.finance.payment.capture.service;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Step saga di capture: conferma l'importo autorizzato con il processore (mockato).
 */
@Service
@Slf4j
public class CaptureService {

    /**
     * Cattura i fondi precedentemente autorizzati. Il mock demo ha successo salvo importo zero.
     */
    public CaptureResult capture(UUID paymentId, BigDecimal amount, String currency, String authorizationCode) {
        log.info("Capturing payment {} for {} {} (auth={})", paymentId, amount, currency, authorizationCode);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CaptureResult.failure("Invalid capture amount");
        }

        return CaptureResult.success("CAP-" + paymentId.toString().substring(0, 8).toUpperCase());
    }

    /** Esito di un'operazione di capture. */
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
