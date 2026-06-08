package com.finance.payment.capture;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.capture.kafka.CaptureEventConsumer;
import com.finance.payment.capture.service.CaptureService;
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
 * Unit tests for {@link CaptureEventConsumer}.
 */
@ExtendWith(MockitoExtension.class)
class CaptureEventConsumerTest {

    @Mock
    private CaptureService captureService;
    @Mock
    private SagaEventDedupService dedupService;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private CaptureEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new CaptureEventConsumer(captureService, dedupService, kafkaTemplate, objectMapper);
    }

    @Test
    void shouldCaptureOnPaymentAuthorized() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.PAYMENT_AUTHORIZED,
                paymentId, buildPayload(paymentId, "200.00", "EUR", "AUTH-XYZ"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(captureService.capture(eq(paymentId), any(BigDecimal.class), eq("EUR"), eq("AUTH-XYZ")))
                .thenReturn(CaptureService.CaptureResult.builder().success(true).captureReference("CAP-1").build());

        consumer.handle(record);

        verify(captureService).capture(eq(paymentId), eq(new BigDecimal("200.00")), eq("EUR"), eq("AUTH-XYZ"));
        verify(kafkaTemplate).send(anyString(), eq(paymentId.toString()), anyString());
    }

    @Test
    void shouldPublishCaptureFailedWhenCaptureServiceFails() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.PAYMENT_AUTHORIZED,
                paymentId, buildPayload(paymentId, "0.00", "EUR", "AUTH-ZZZ"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(captureService.capture(any(), any(), any(), any()))
                .thenReturn(CaptureService.CaptureResult.builder().success(false).failureReason("Zero amount").build());

        consumer.handle(record);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), anyString(), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("CaptureFailed");
    }

    @Test
    void shouldSkipNonAuthorizedEvents() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.PAYMENT_INITIATED,
                paymentId, buildPayload(paymentId, "50.00", "EUR", "AUTH-1"));

        consumer.handle(record);

        verifyNoInteractions(dedupService, captureService);
    }

    @Test
    void shouldSkipDuplicateEvent() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.PAYMENT_AUTHORIZED,
                paymentId, buildPayload(paymentId, "100.00", "EUR", "AUTH-2"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(false);

        consumer.handle(record);

        verifyNoInteractions(captureService);
    }

    @Test
    void shouldWrapExceptionInIllegalStateException() {
        UUID paymentId = UUID.randomUUID();
        ConsumerRecord<String, String> record = buildRecord(PaymentEventType.PAYMENT_AUTHORIZED,
                paymentId, buildPayload(paymentId, "100.00", "EUR", "AUTH-3"));

        when(dedupService.registerIfNew(anyString(), eq(paymentId), anyString())).thenReturn(true);
        when(captureService.capture(any(), any(), any(), any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> consumer.handle(record))
                .isInstanceOf(IllegalStateException.class);
    }

    private Map<String, Object> buildPayload(UUID paymentId, String amount, String currency, String authCode) {
        Map<String, Object> p = new HashMap<>();
        p.put("paymentId", paymentId.toString());
        p.put("merchantId", UUID.randomUUID().toString());
        p.put("amount", amount);
        p.put("currency", currency);
        p.put("authorizationCode", authCode);
        return p;
    }

    private ConsumerRecord<String, String> buildRecord(PaymentEventType type, UUID paymentId,
                                                        Map<String, Object> payload) {
        PaymentEvent event = PaymentEvent.of(type, paymentId, payload);
        String json = objectMapper.writeValueAsString(event);
        return new ConsumerRecord<>("payment.events", 0, 0L, paymentId.toString(), json);
    }
}
