package com.finance.payment.capture.service;

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
 * Step saga di capture: conferma l'importo autorizzato con il processore (mock o Stripe).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CaptureService {

    private final PaymentProcessor paymentProcessor;

    /** Cattura i fondi precedentemente autorizzati. */
    public CaptureResult capture(UUID paymentId, BigDecimal amount, String currency, String authorizationCode) {
        log.info("Capturing payment {} for {} {} (auth={})", paymentId, amount, currency, authorizationCode);
        ProcessorResult result = paymentProcessor.capture(paymentId, amount, currency, authorizationCode);
        if (!result.success()) {
            return CaptureResult.failure(result.failureReason());
        }
        return CaptureResult.success(result.reference());
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
