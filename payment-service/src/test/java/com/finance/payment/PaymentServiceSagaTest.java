package com.finance.payment;

import com.finance.payment.domain.Payment;
import com.finance.payment.domain.PaymentStatus;
import com.finance.payment.repository.PaymentAuditEventRepository;
import com.finance.payment.repository.PaymentOutboxRepository;
import com.finance.payment.repository.PaymentRepository;
import com.finance.payment.service.IdempotencyService;
import com.finance.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceSagaTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentOutboxRepository outboxRepository;
    @Mock
    private PaymentAuditEventRepository auditEventRepository;
    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldKeepCapturedStatusOnSettlementFailedUntilRefund() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.initiate("key-1", UUID.randomUUID(), new BigDecimal("9999.00"), "EUR", null, null);
        setPaymentId(payment, paymentId);
        payment.authorize();
        payment.capture();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        paymentService.handleSettlementFailed(paymentId, "Acquirer rejected");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(auditEventRepository).save(any());
    }

    @Test
    void shouldRefundAfterSettlementFailedAudit() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.initiate("key-2", UUID.randomUUID(), new BigDecimal("9999.00"), "EUR", null, null);
        setPaymentId(payment, paymentId);
        payment.authorize();
        payment.capture();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        paymentService.handleSettlementFailed(paymentId, "Acquirer rejected");
        paymentService.handleRefunded(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    private static void setPaymentId(Payment payment, UUID id) throws Exception {
        Field idField = Payment.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(payment, id);
    }
}
