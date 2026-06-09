package com.finance.payment.common.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitari per {@link SagaEventDedupService}: verifica la logica INSERT ON CONFLICT
 * tramite {@link JdbcTemplate} mockato per eventi nuovi e duplicati.
 */
@ExtendWith(MockitoExtension.class)
class SagaEventDedupServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SagaEventDedupService service;

    @BeforeEach
    void setUp() {
        service = new SagaEventDedupService(jdbcTemplate);
    }

    @Test
    void shouldReturnTrueWhenEventIsNew() {
        UUID paymentId = UUID.randomUUID();
        when(jdbcTemplate.update(any(String.class), eq("auth-group"), eq(paymentId), eq("PaymentInitiated")))
                .thenReturn(1);

        boolean isNew = service.registerIfNew("auth-group", paymentId, "PaymentInitiated");

        assertThat(isNew).isTrue();
        verify(jdbcTemplate).update(any(String.class), eq("auth-group"), eq(paymentId), eq("PaymentInitiated"));
    }

    @Test
    void shouldReturnFalseWhenEventIsDuplicate() {
        UUID paymentId = UUID.randomUUID();
        when(jdbcTemplate.update(any(String.class), any(), any(), any()))
                .thenReturn(0);

        boolean isNew = service.registerIfNew("auth-group", paymentId, "PaymentInitiated");

        assertThat(isNew).isFalse();
    }

    @Test
    void shouldPassAllParametersToJdbc() {
        UUID paymentId = UUID.randomUUID();
        String group = "settlement-service";
        String eventType = "PaymentCaptured";
        when(jdbcTemplate.update(any(), any(), any(), any())).thenReturn(1);

        service.registerIfNew(group, paymentId, eventType);

        verify(jdbcTemplate).update(any(String.class), eq(group), eq(paymentId), eq(eventType));
    }
}
