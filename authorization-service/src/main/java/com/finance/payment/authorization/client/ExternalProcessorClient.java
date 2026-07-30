package com.finance.payment.authorization.client;

import com.finance.payment.authorization.dto.AuthorizationResult;
import com.finance.payment.common.exception.ProcessorUnavailableException;
import com.finance.payment.common.processor.PaymentProcessor;
import com.finance.payment.common.processor.ProcessorResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client Resilience4j-wrapped verso {@link PaymentProcessor} (mock locale o Stripe).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalProcessorClient {

    private final PaymentProcessor paymentProcessor;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /** Autorizza (blocca) i fondi. */
    public AuthorizationResult authorize(
            UUID paymentId, BigDecimal amount, String currency, String paymentMethodId) {
        try {
            ProcessorResult result = executeWithResilience(
                    () -> paymentProcessor.authorize(paymentId, amount, currency, paymentMethodId));
            return result.success()
                    ? AuthorizationResult.success(result.reference())
                    : AuthorizationResult.failure(result.failureReason());
        } catch (ProcessorUnavailableException e) {
            log.warn("Authorization unavailable for payment {}: {}", paymentId, e.getMessage());
            return AuthorizationResult.failure(e.getMessage());
        }
    }

    /** Annulla un'autorizzazione precedente — percorso di compensazione quando la capture fallisce. */
    public void voidAuthorization(UUID paymentId, String authorizationCode) {
        executeWithResilience(() -> paymentProcessor.voidAuthorization(paymentId, authorizationCode));
    }

    private ProcessorResult executeWithResilience(Supplier<ProcessorResult> supplier) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment-processor");
        Retry retry = retryRegistry.retry("payment-processor");

        Supplier<ProcessorResult> decorated = Decorators
                .ofSupplier(supplier)
                .withCircuitBreaker(cb)
                .withRetry(retry)
                .decorate();

        try {
            return decorated.get();
        } catch (Exception e) {
            throw new ProcessorUnavailableException(
                    "Processor unavailable after retries: " + e.getMessage(), e);
        }
    }
}
