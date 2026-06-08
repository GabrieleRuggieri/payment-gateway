package com.finance.payment.authorization.client;

import com.finance.payment.authorization.dto.AuthorizationResult;
import com.finance.payment.common.exception.ProcessorUnavailableException;
import com.finance.payment.common.exception.TemporaryProcessorException;
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
 * Resilience4j-wrapped client for an external payment processor (mocked for the demo).
 * <p>
 * Mock rules: amounts &gt; 9999 trigger {@link TemporaryProcessorException} to exercise retries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalProcessorClient {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Authorize (hold) funds. Replace {@link #callProcessor} with a real HTTP integration.
     */
    public AuthorizationResult authorize(UUID paymentId, BigDecimal amount, String currency) {
        return executeWithResilience(() -> callProcessor(paymentId, amount, currency));
    }

    /**
     * Void a prior authorization — compensation path when capture fails.
     */
    public void voidAuthorization(UUID paymentId, String authorizationCode) {
        executeWithResilience(() -> {
            log.info("Processor void OK payment={} authCode={}", paymentId, authorizationCode);
            return AuthorizationResult.success("VOID-" + paymentId.toString().substring(0, 8));
        });
    }

    private AuthorizationResult executeWithResilience(Supplier<AuthorizationResult> supplier) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment-processor");
        Retry retry = retryRegistry.retry("payment-processor");

        Supplier<AuthorizationResult> decorated = Decorators
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

    private AuthorizationResult callProcessor(UUID paymentId, BigDecimal amount, String currency) {
        log.debug("Authorizing payment {} amount {} {}", paymentId, amount, currency);

        if (amount.compareTo(new BigDecimal("9999")) > 0) {
            throw new TemporaryProcessorException("Limit exceeded");
        }

        return AuthorizationResult.success(
                "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
