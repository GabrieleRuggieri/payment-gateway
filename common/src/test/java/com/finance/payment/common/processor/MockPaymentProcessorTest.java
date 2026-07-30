package com.finance.payment.common.processor;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica le soglie demo del {@link MockPaymentProcessor}. */
class MockPaymentProcessorTest {

    private final MockPaymentProcessor processor = new MockPaymentProcessor(new ProcessorProperties());

    @Test
    void authorizeSucceedsAtLimit() {
        var result = processor.authorize(UUID.randomUUID(), new BigDecimal("9999.00"), "EUR", null);
        assertThat(result.success()).isTrue();
    }

    @Test
    void authorizeFailsAboveLimit() {
        var result = processor.authorize(UUID.randomUUID(), new BigDecimal("10000"), "EUR", null);
        assertThat(result.success()).isFalse();
    }

    @Test
    void captureFailsBetweenCaptureAndAuthThreshold() {
        var result = processor.capture(UUID.randomUUID(), new BigDecimal("9000"), "EUR", "AUTH-X");
        assertThat(result.success()).isFalse();
    }

    @Test
    void toMinorUnitsRoundsHalfUp() {
        assertThat(StripePaymentProcessor.toMinorUnits(new BigDecimal("19.99"))).isEqualTo(1999L);
        assertThat(StripePaymentProcessor.toMinorUnits(new BigDecimal("10.005"))).isEqualTo(1001L);
    }
}
