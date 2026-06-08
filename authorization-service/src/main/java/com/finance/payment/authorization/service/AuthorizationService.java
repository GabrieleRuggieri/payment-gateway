package com.finance.payment.authorization.service;

import com.finance.payment.authorization.client.ExternalProcessorClient;
import com.finance.payment.authorization.dto.AuthorizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final ExternalProcessorClient processorClient;

    /**
     * TODO: Add idempotent processing guard (dedupe by paymentId + eventType in Redis/DB).
     */
    public AuthorizationResult authorize(UUID paymentId, BigDecimal amount, String currency) {
        return processorClient.authorize(paymentId, amount, currency);
    }

    /**
     * TODO: Compensation — void/release authorization when CaptureFailed is published.
     */
    public void voidAuthorization(UUID paymentId, String authorizationCode) {
        throw new UnsupportedOperationException("TODO: call processor void API");
    }
}
