package com.finance.payment.notification.service;

import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Set;

/**
 * Invia webhook ai merchant per eventi terminali e milestone della saga.
 * <p>
 * La deduplicazione usa Redis ({@code SET NX} con TTL) — appropriata per le notifiche dove
 * webhook duplicati sono fastidiosi ma non pericolosi finanziariamente. La dedup critica resta su PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

    private static final Set<String> NOTIFY_EVENT_TYPES = Set.of(
            PaymentEventType.PAYMENT_AUTHORIZED.wireName(),
            PaymentEventType.PAYMENT_CAPTURED.wireName(),
            PaymentEventType.PAYMENT_SETTLED.wireName(),
            PaymentEventType.PAYMENT_REFUNDED.wireName(),
            PaymentEventType.AUTHORIZATION_FAILED.wireName(),
            PaymentEventType.CAPTURE_FAILED.wireName(),
            PaymentEventType.SETTLEMENT_FAILED.wireName()
    );

    private static final Duration DEDUP_TTL = Duration.ofHours(24);

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${notification.webhook.default-url}")
    private String defaultWebhookUrl;

    /**
     * Invia via POST l'evento JSON al webhook del merchant se non già consegnato per la coppia pagamento+evento.
     */
    public void notifyMerchant(PaymentEvent event) {
        if (!NOTIFY_EVENT_TYPES.contains(event.getEventType())) {
            log.trace("Skipping non-notifiable event {}", event.getEventType());
            return;
        }

        String dedupKey = "webhook:%s:%s".formatted(event.getPaymentId(), event.getEventType());
        Boolean firstDelivery = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", DEDUP_TTL);

        if (Boolean.FALSE.equals(firstDelivery)) {
            log.debug("Webhook already sent for {}", dedupKey);
            return;
        }

        log.info("Sending webhook for payment {} event {}", event.getPaymentId(), event.getEventType());

        try {
            restTemplate.postForEntity(defaultWebhookUrl, event, Void.class);
        } catch (Exception e) {
            redisTemplate.delete(dedupKey);
            log.warn("Webhook delivery failed for {}: {}", dedupKey, e.getMessage());
            throw e;
        }
    }
}
