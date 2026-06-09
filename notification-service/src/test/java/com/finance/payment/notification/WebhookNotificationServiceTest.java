package com.finance.payment.notification;

import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.notification.service.WebhookNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookNotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WebhookNotificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "defaultWebhookUrl", "http://localhost:8099/webhooks/payments");
    }

    @Test
    void shouldDeliverWebhookForNotifiableEvent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID paymentId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.of(
                PaymentEventType.PAYMENT_SETTLED,
                paymentId,
                Map.of("paymentId", paymentId.toString())
        );

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(true);

        service.notifyMerchant(event);

        verify(restTemplate).postForEntity(eq("http://localhost:8099/webhooks/payments"), eq(event), eq(Void.class));
    }

    @Test
    void shouldSkipDuplicateWebhookDelivery() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UUID paymentId = UUID.randomUUID();
        PaymentEvent event = PaymentEvent.of(
                PaymentEventType.PAYMENT_CAPTURED,
                paymentId,
                Map.of("paymentId", paymentId.toString())
        );

        when(valueOperations.setIfAbsent(anyString(), eq("1"), any())).thenReturn(false);

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

        verify(redisTemplate, never()).opsForValue();
        verify(restTemplate, never()).postForEntity(anyString(), any(), eq(Void.class));
    }
}
