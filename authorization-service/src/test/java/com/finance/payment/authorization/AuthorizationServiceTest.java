package com.finance.payment.authorization;

import com.finance.payment.authorization.client.ExternalProcessorClient;
import com.finance.payment.authorization.dto.AuthorizationResult;
import com.finance.payment.authorization.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthorizationService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private ExternalProcessorClient processorClient;

    @InjectMocks
    private AuthorizationService service;

    @Test
    void shouldDelegateAuthorizeToProcessor() {
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(processorClient.authorize(paymentId, amount, "EUR"))
                .thenReturn(AuthorizationResult.success("AUTH-ABCD1234"));

        AuthorizationResult result = service.authorize(paymentId, amount, "EUR");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAuthorizationCode()).isEqualTo("AUTH-ABCD1234");
        verify(processorClient).authorize(paymentId, amount, "EUR");
    }

    @Test
    void shouldPropagateFailureFromProcessor() {
        UUID paymentId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("99.00");
        when(processorClient.authorize(paymentId, amount, "USD"))
                .thenReturn(AuthorizationResult.failure("Processor declined"));

        AuthorizationResult result = service.authorize(paymentId, amount, "USD");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Processor declined");
    }

    @Test
    void shouldDelegateVoidAuthorizationToProcessor() {
        UUID paymentId = UUID.randomUUID();
        service.voidAuthorization(paymentId, "AUTH-CODE");
        verify(processorClient).voidAuthorization(paymentId, "AUTH-CODE");
    }
}
