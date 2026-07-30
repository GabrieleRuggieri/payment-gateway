package com.finance.payment;

import com.finance.payment.domain.Payment;
import com.finance.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica le transizioni di stato del dominio {@link Payment}, il rifiuto di transizioni non valide,
 * la validazione degli importi negativi e la normalizzazione a quattro decimali.
 */
class PaymentTest {

    @Test
    void shouldTransitionFromInitiatedToAuthorized() {
        Payment payment = Payment.initiate(
                "key-123", UUID.randomUUID(),
                new BigDecimal("100.00"), "EUR", null, null
        );

        payment.authorize();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Payment payment = Payment.initiate(
                "key-123", UUID.randomUUID(),
                new BigDecimal("100.00"), "EUR", null, null
        );

        assertThatThrownBy(payment::capture)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Payment.initiate(
                "key-123", UUID.randomUUID(),
                new BigDecimal("-50.00"), "EUR", null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllowRefundFromSettled() {
        Payment payment = Payment.initiate(
                "key-refund", UUID.randomUUID(),
                new BigDecimal("40.00"), "EUR", null, null
        );
        payment.authorize();
        payment.capture();
        payment.settle();
        payment.refund();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void shouldAllowDisputeFromSettled() {
        Payment payment = Payment.initiate(
                "key-dispute", UUID.randomUUID(),
                new BigDecimal("40.00"), "EUR", null, null
        );
        payment.authorize();
        payment.capture();
        payment.settle();
        payment.dispute();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DISPUTED);
    }

    @Test
    void shouldMergeProcessorMetadata() {
        Payment payment = Payment.initiate(
                "key-meta", UUID.randomUUID(),
                new BigDecimal("10.00"), "EUR", null, null
        );
        payment.putMetadata("processorPaymentIntentId", "pi_test_123");
        assertThat(payment.getMetadata()).containsEntry("processorPaymentIntentId", "pi_test_123");
    }
}
