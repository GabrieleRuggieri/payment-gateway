package com.finance.payment.settlement;

import com.finance.payment.settlement.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitari per {@link SettlementService}: percorso di successo, fallimento oltre soglia
 * e generazione dei riferimenti di settlement e refund.
 */
class SettlementServiceTest {

    private SettlementService service;

    @BeforeEach
    void setUp() {
        service = new SettlementService();
    }

    // ── settle() ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "settle succeeds for amount {0}")
    @ValueSource(strings = {"1.00", "100.00", "4999.99", "4999.98"})
    void shouldSettleSuccessfullyBelowThreshold(String amountStr) {
        UUID paymentId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        var result = service.settle(paymentId, merchantId, new BigDecimal(amountStr), "EUR");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSettlementReference())
                .isNotNull()
                .startsWith("SET-");
    }

    @ParameterizedTest(name = "settle fails for amount {0}")
    @ValueSource(strings = {"5000.00", "5001.00", "9000.00"})
    void shouldFailSettlementAboveThreshold(String amountStr) {
        UUID paymentId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        var result = service.settle(paymentId, merchantId, new BigDecimal(amountStr), "EUR");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).isNotBlank();
        assertThat(result.getSettlementReference()).isNull();
    }

    @Test
    void settlementReferenceIncludesPaymentIdPrefix() {
        UUID paymentId = UUID.randomUUID();
        var result = service.settle(paymentId, UUID.randomUUID(), new BigDecimal("100.00"), "USD");

        String expectedPrefix = "SET-" + paymentId.toString().substring(0, 8).toUpperCase();
        assertThat(result.getSettlementReference()).isEqualTo(expectedPrefix);
    }

    // ── refund() ────────────────────────────────────────────────────────────

    @Test
    void shouldRefundSuccessfully() {
        UUID paymentId = UUID.randomUUID();
        var result = service.refund(paymentId, new BigDecimal("200.00"), "GBP");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRefundReference())
                .isNotNull()
                .startsWith("REF-");
    }

    @Test
    void refundReferenceIncludesPaymentIdPrefix() {
        UUID paymentId = UUID.randomUUID();
        var result = service.refund(paymentId, new BigDecimal("50.00"), "EUR");

        String expectedPrefix = "REF-" + paymentId.toString().substring(0, 8).toUpperCase();
        assertThat(result.getRefundReference()).isEqualTo(expectedPrefix);
    }
}
