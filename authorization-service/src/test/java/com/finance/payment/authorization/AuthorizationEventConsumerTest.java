package com.finance.payment.authorization;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.authorization.dto.AuthorizationResult;
import com.finance.payment.authorization.kafka.AuthorizationEventConsumer;
import com.finance.payment.authorization.service.AuthorizationService;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.common.saga.SagaEventDedupService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link AuthorizationEventConsumer}: mock delle dipendenze e invocazione diretta del listener
 * per verificare autorizzazione, compensazione su capture fallita, deduplicazione e gestione errori.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationEventConsumerTest {

    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private SagaEventDedupService dedupService;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private AuthorizationEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new AuthorizationEventConsumer(authorizationService, dedupService,
                kafkaTemplate, objectMapper);
    }

    // ── PAYMENT_INITIATED: autorizzazione riuscita ────────────────────────────

    @Test
    void shouldAuthorizeOnPaymentInitiated() {
        UUID paymentId = UUID.randomUUID();
        Map<String, Object> payload = buildPayload(paymentId, "150.00", "EUR");
        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.PAYMENT_INITIATED, paymentId, payload);

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(authorizationService.authorize(eq(paymentId), any(BigDecimal.class), eq("EUR")))
                .thenReturn(AuthorizationResult.success("AUTH-ABCD"));

        consumer.handlePaymentEvent(record);

        verify(authorizationService).authorize(eq(paymentId), eq(new BigDecimal("150.00")), eq("EUR"));
        verify(kafkaTemplate).send(anyString(), eq(paymentId.toString()), anyString());
    }

    @Test
    void shouldPublishAuthorizationFailedWhenProcessorDeclines() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_INITIATED, paymentId, buildPayload(paymentId, "200.00", "USD"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(authorizationService.authorize(any(), any(), any()))
                .thenReturn(AuthorizationResult.failure("Declined"));

        consumer.handlePaymentEvent(record);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("AuthorizationFailed");
    }

    // ── CAPTURE_FAILED: percorso di compensazione ──────────────────────────────

    @Test
    void shouldVoidAuthorizationOnCaptureFailed() {
        UUID paymentId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(buildPayload(paymentId, "100.00", "EUR"));
        payload.put("authorizationCode", "AUTH-VOID");

        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.CAPTURE_FAILED, paymentId, payload);
        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);

        consumer.handlePaymentEvent(record);

        verify(authorizationService).voidAuthorization(eq(paymentId), eq("AUTH-VOID"));
    }

    // ── Idempotenza: eventi duplicati ─────────────────────────────────────────

    @Test
    void shouldSkipProcessingForDuplicateEvent() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_INITIATED, paymentId, buildPayload(paymentId, "50.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(false);

        consumer.handlePaymentEvent(record);

        verifyNoInteractions(authorizationService);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    // ── Tipi di evento non pertinenti ───────────────────────────────────────

    @Test
    void shouldIgnoreUnrelatedEventTypes() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_SETTLED, paymentId, buildPayload(paymentId, "50.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);

        consumer.handlePaymentEvent(record);

        verifyNoInteractions(authorizationService);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    // ── Gestione errori ───────────────────────────────────────────────────────

    @Test
    void shouldWrapExceptionInIllegalStateException() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(
                PaymentEventType.PAYMENT_INITIATED, paymentId, buildPayload(paymentId, "50.00", "EUR"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(authorizationService.authorize(any(), any(), any()))
                .thenThrow(new RuntimeException("Processor down"));

        assertThatThrownBy(() -> consumer.handlePaymentEvent(record))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildPayload(UUID paymentId, String amount, String currency) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId.toString());
        payload.put("merchantId", UUID.randomUUID().toString());
        payload.put("amount", amount);
        payload.put("currency", currency);
        return payload;
    }

    private ConsumerRecord<String, String> buildRecord(PaymentEventType type, UUID paymentId,
                                                        Map<String, Object> payload) {
        PaymentEvent event = PaymentEvent.of(type, paymentId, payload);
        String json = objectMapper.writeValueAsString(event);
        return new ConsumerRecord<>("payment.events", 0, 0L, paymentId.toString(), json);
    }
}
