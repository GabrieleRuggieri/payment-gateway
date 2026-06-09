package com.finance.payment.settlement;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.saga.SagaEventDedupService;
import com.finance.payment.settlement.kafka.SettlementEventConsumer;
import com.finance.payment.settlement.service.SettlementService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link SettlementEventConsumer}: percorso felice, compensazione
 * ({@code SETTLEMENT_FAILED} → {@code PAYMENT_REFUNDED}), deduplicazione e routing degli eventi.
 */
@ExtendWith(MockitoExtension.class)
class SettlementEventConsumerTest {

    @Mock
    private SettlementService settlementService;
    @Mock
    private SagaEventDedupService dedupService;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private SettlementEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new SettlementEventConsumer(settlementService, dedupService, kafkaTemplate, objectMapper);
    }

    // ── Percorso felice ───────────────────────────────────────────────────────

    @Test
    void shouldSettleOnPaymentCaptured() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_CAPTURED, paymentId, buildCapturePayload(paymentId, "300.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(settlementService.settle(eq(paymentId), any(UUID.class), any(), eq("EUR")))
                .thenReturn(SettlementService.SettlementResult.builder()
                        .success(true).settlementReference("SET-ABCD").build());

        consumer.handle(record);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), eq(paymentId.toString()), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("PaymentSettled");
    }

    @Test
    void shouldPublishSettlementFailedWhenServiceFails() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_CAPTURED, paymentId, buildCapturePayload(paymentId, "9000.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(settlementService.settle(any(), any(), any(), any()))
                .thenReturn(SettlementService.SettlementResult.builder()
                        .success(false).failureReason("Limit exceeded").build());

        consumer.handle(record);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("SettlementFailed");
    }

    // ── Percorso di compensazione ─────────────────────────────────────────────

    @Test
    void shouldRefundOnSettlementFailed() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.SETTLEMENT_FAILED, paymentId, buildCapturePayload(paymentId, "5500.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(settlementService.refund(eq(paymentId), any(), eq("EUR")))
                .thenReturn(SettlementService.RefundResult.builder()
                        .success(true).refundReference("REF-ABCD").build());

        consumer.handle(record);

        verify(settlementService).refund(eq(paymentId), any(), eq("EUR"));
        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("PaymentRefunded");
    }

    // ── Deduplicazione e routing ──────────────────────────────────────────────

    @Test
    void shouldSkipDuplicateEvent() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_CAPTURED, paymentId, buildCapturePayload(paymentId, "100.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(false);

        consumer.handle(record);

        verifyNoInteractions(settlementService);
    }

    @Test
    void shouldIgnoreUnrelatedEvents() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_INITIATED, paymentId, buildCapturePayload(paymentId, "100.00", "EUR"));

        consumer.handle(record);

        verifyNoInteractions(dedupService, settlementService);
    }

    @Test
    void shouldWrapExceptionInIllegalStateException() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_CAPTURED, paymentId, buildCapturePayload(paymentId, "100.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(settlementService.settle(any(), any(), any(), any())).thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> consumer.handle(record))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildCapturePayload(UUID paymentId, String amount, String currency) {
        Map<String, Object> p = new HashMap<>();
        p.put("paymentId", paymentId.toString());
        p.put("merchantId", UUID.randomUUID().toString());
        p.put("amount", amount);
        p.put("currency", currency);
        p.put("authorizationCode", "AUTH-TEST");
        p.put("captureReference", "CAP-TEST");
        return p;
    }

    private ConsumerRecord<String, String> buildRecord(PaymentEventType type, UUID paymentId,
                                                        Map<String, Object> payload) {
        PaymentEvent event = PaymentEvent.of(type, paymentId, payload);
        String json = objectMapper.writeValueAsString(event);
        return new ConsumerRecord<>("payment.events", 0, 0L, paymentId.toString(), json);
    }
}
