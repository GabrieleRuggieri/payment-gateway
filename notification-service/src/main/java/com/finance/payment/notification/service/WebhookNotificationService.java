package com.finance.payment.notification.service;

import com.finance.payment.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookNotificationService {

    private final RestTemplate restTemplate;

    @Value("${notification.webhook.default-url}")
    private String defaultWebhookUrl;

    /**
     * TODO:
     * - Resolve merchant-specific webhook URL
     * - Sign payload (HMAC-SHA256) for merchant verification
     * - Retry with exponential backoff; store delivery status in Redis
     * - Deduplicate notifications by paymentId + eventType
     */
    public void notifyMerchant(PaymentEvent event) {
        log.info("Sending webhook for payment {} event {}", event.getPaymentId(), event.getEventType());

        // TODO: replace with resilient WebClient + retry
        try {
            restTemplate.postForEntity(defaultWebhookUrl, event, Void.class);
        } catch (Exception e) {
            log.warn("Webhook delivery failed: {}", e.getMessage());
        }
    }
}
