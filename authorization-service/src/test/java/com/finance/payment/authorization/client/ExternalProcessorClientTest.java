package com.finance.payment.authorization.client;

import com.finance.payment.authorization.dto.AuthorizationResult;
import com.finance.payment.common.processor.MockPaymentProcessor;
import com.finance.payment.common.processor.ProcessorProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica le risposte di {@link ExternalProcessorClient} con mock processor:
 * autorizzazione entro il limite e rifiuto oltre soglia.
 */
class ExternalProcessorClientTest {

    private ExternalProcessorClient client;

    @BeforeEach
    void setUp() {
        client = new ExternalProcessorClient(
                new MockPaymentProcessor(new ProcessorProperties()),
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults());
    }

    @Test
    void shouldAuthorizeAtProcessorLimit() {
        AuthorizationResult result = client.authorize(
                UUID.randomUUID(), new BigDecimal("9999.00"), "EUR", null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAuthorizationCode()).startsWith("AUTH-");
    }

    @Test
    void shouldReturnFailureWhenAmountExceedsProcessorLimit() {
        AuthorizationResult result = client.authorize(
                UUID.randomUUID(), new BigDecimal("10000.00"), "EUR", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Limit exceeded");
    }
}
