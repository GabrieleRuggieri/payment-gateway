package com.finance.payment.notification;

import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.notification.service.WebhookNotificationService;
import com.finance.payment.notification.webhook.MerchantWebhookRepository;
import com.finance.payment.notification.webhook.WebhookDeliveryRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica {@link WebhookNotificationService}: enqueue su coda persistente, dedup e skip eventi non notificabili.
 */
@ExtendWith(MockitoExtension.class)
class WebhookNotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private MerchantWebhookRepository merchantWebhookRepository;
    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @InjectMocks
    private WebhookNotificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultWebhookUrl", "http://localhost:8099/webhooks/payments");
        ReflectionTestUtils.setField(service, "forceDefaultUrl", true);
        ReflectionTestUtils.setField(service, "defaultSecret", "whsec_demo");
    }

    @Test
    void shouldEnqueueWebhookForNotifiableEvent() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID merchantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        PaymentEvent event = PaymentEvent.of(
                PaymentEventType.PAYMENT_SETTLED,
                paymentId,
                Map.of("paymentId", paymentId.toString(), "merchantId", merchantId.toString())
        );

        when(merchantWebhookRepository.findActive(merchantId)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"ok\":true}");
        when(deliveryRepository.enqueue(eq(merchantId), eq(paymentId), anyString(), anyString(), anyString()))
                .thenReturn(true);

        service.notifyMerchant(event);

        verify(deliveryRepository).enqueue(
                eq(merchantId),
                eq(paymentId),
                eq(PaymentEventType.PAYMENT_SETTLED.wireName()),
                eq("{\"ok\":true}"),
                eq("http://localhost:8099/webhooks/payments"));
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(Void.class));
    }

    @Test
    void shouldSkipDuplicateWebhookDelivery() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID merchantId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        PaymentEvent event = PaymentEvent.of(
                PaymentEventType.PAYMENT_CAPTURED,
                paymentId,
                Map.of("paymentId", paymentId.toString(), "merchantId", merchantId.toString())
        );

        when(merchantWebhookRepository.findActive(merchantId)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(event)).thenReturn("{}");
        when(deliveryRepository.enqueue(any(), any(), anyString(), anyString(), anyString())).thenReturn(false);

        service.notifyMerchant(event);

        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(Void.class));
    }

    @Test
    void shouldIgnoreNonNotifiableEvents() {
        UUID paymentId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.of(
                PaymentEventType.PAYMENT_INITIATED,
                paymentId,
                Map.of("paymentId", paymentId.toString())
        );

        service.notifyMerchant(event);

        verify(deliveryRepository, never()).enqueue(any(), any(), anyString(), anyString(), anyString());
    }
}
