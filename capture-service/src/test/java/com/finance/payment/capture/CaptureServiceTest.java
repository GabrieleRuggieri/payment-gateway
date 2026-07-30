package com.finance.payment.capture;

import com.finance.payment.capture.service.CaptureService;
import com.finance.payment.common.processor.MockPaymentProcessor;
import com.finance.payment.common.processor.ProcessorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitari per {@link CaptureService}: capture riuscita, formato del riferimento di capture
 * e validazione degli importi non positivi (via mock processor).
 */
class CaptureServiceTest {

    private CaptureService service;

    @BeforeEach
    void setUp() {
        service = new CaptureService(new MockPaymentProcessor(new ProcessorProperties()));
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

    @Test
    void shouldFailCaptureAboveMockThreshold() {
        var result = service.capture(UUID.randomUUID(), new BigDecimal("9000.00"), "EUR", "AUTH-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).contains("capture");
    }
}
