package com.finance.payment.capture;

import com.finance.payment.capture.service.CaptureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CaptureService}.
 */
class CaptureServiceTest {

    private CaptureService service;

    @BeforeEach
    void setUp() {
        service = new CaptureService();
    }

    @Test
    void shouldCaptureSuccessfully() {
        UUID paymentId = UUID.randomUUID();
        var result = service.capture(paymentId, new BigDecimal("150.00"), "EUR", "AUTH-1234");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCaptureReference())
                .isNotNull()
                .startsWith("CAP-");
    }

    @Test
    void captureReferenceIncludesPaymentIdPrefix() {
        UUID paymentId = UUID.randomUUID();
        var result = service.capture(paymentId, new BigDecimal("50.00"), "USD", "AUTH-ABCD");

        String expectedPrefix = "CAP-" + paymentId.toString().substring(0, 8).toUpperCase();
        assertThat(result.getCaptureReference()).isEqualTo(expectedPrefix);
    }

    @Test
    void shouldFailForZeroAmount() {
        var result = service.capture(UUID.randomUUID(), BigDecimal.ZERO, "EUR", "AUTH-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isNotBlank();
    }

    @Test
    void shouldFailForNegativeAmount() {
        var result = service.capture(UUID.randomUUID(), new BigDecimal("-1.00"), "EUR", "AUTH-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isNotBlank();
    }
}
