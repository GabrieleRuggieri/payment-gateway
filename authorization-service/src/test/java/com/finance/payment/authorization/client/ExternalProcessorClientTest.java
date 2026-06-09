package com.finance.payment.authorization.client;

import com.finance.payment.authorization.dto.AuthorizationResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica le risposte di {@link ExternalProcessorClient}: autorizzazione entro il limite del processore
 * e rifiuto quando l'importo supera la soglia consentita.
 */
class ExternalProcessorClientTest {

    private ExternalProcessorClient client;

    @BeforeEach
    void setUp() {
        client = new ExternalProcessorClient(CircuitBreakerRegistry.ofDefaults(), RetryRegistry.ofDefaults());
    }

    @Test
    void shouldAuthorizeAtProcessorLimit() {
        AuthorizationResult result = client.authorize(UUID.randomUUID(), new BigDecimal("9999.00"), "EUR");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAuthorizationCode()).startsWith("AUTH-");
    }

    @Test
    void shouldReturnFailureWhenAmountExceedsProcessorLimit() {
        AuthorizationResult result = client.authorize(UUID.randomUUID(), new BigDecimal("10000.00"), "EUR");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Limit exceeded");
    }
}
