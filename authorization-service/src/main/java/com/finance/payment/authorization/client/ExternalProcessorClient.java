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

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalProcessorClient {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * TODO: Replace mock with RestTemplate/WebClient call to external processor.
     * Mock rule from README: fail with TemporaryProcessorException when amount > 9999.
     */
    public AuthorizationResult authorize(UUID paymentId, BigDecimal amount, String currency) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment-processor");
        Retry retry = retryRegistry.retry("payment-processor");

        Supplier<AuthorizationResult> decorated = Decorators
                .ofSupplier(() -> callProcessor(paymentId, amount, currency))
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
