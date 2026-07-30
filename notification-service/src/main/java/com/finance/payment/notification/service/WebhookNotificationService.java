package com.finance.payment.notification.service;

import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import com.finance.payment.notification.webhook.MerchantWebhookRepository;
import com.finance.payment.notification.webhook.WebhookDeliveryRepository;
import com.finance.payment.notification.webhook.WebhookSigner;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Accoda webhook merchant su PostgreSQL e li consegna con HMAC + retry esponenziale fino a DLQ ({@code DEAD}).
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
            PaymentEventType.PAYMENT_DISPUTED.wireName(),
            PaymentEventType.AUTHORIZATION_FAILED.wireName(),
            PaymentEventType.CAPTURE_FAILED.wireName(),
            PaymentEventType.SETTLEMENT_FAILED.wireName()
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MerchantWebhookRepository merchantWebhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    @Value("${notification.webhook.default-url}")
    private String defaultWebhookUrl;

    @Value("${notification.webhook.force-default-url:false}")
    private boolean forceDefaultUrl;

    @Value("${notification.webhook.default-secret:whsec_demo_payment_gateway_local}")
    private String defaultSecret;

    /** Accoda l'evento per consegna asincrona (dedup su payment_id + event_type). */
    public void notifyMerchant(PaymentEvent event) {
        if (!NOTIFY_EVENT_TYPES.contains(event.getEventType())) {
            log.trace("Skipping non-notifiable event {}", event.getEventType());
            return;
        }

        UUID merchantId = resolveMerchantId(event);
        UUID paymentId = event.getPaymentId();

        var merchantHook = merchantWebhookRepository.findActive(merchantId);
        String url = forceDefaultUrl || merchantHook.isEmpty()
                ? defaultWebhookUrl
                : merchantHook.get().webhookUrl();

        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            boolean enqueued = deliveryRepository.enqueue(
                    merchantId, paymentId, event.getEventType(), payloadJson, url);
            if (!enqueued) {
                log.debug("Webhook already queued/delivered for {} {}", paymentId, event.getEventType());
                return;
            }
            log.info("Queued webhook for payment {} event {} → {}", paymentId, event.getEventType(), url);
        } catch (Exception e) {
            log.warn("Failed to enqueue webhook for {}: {}", paymentId, e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /** Retry periodico delle delivery PENDING scadute. */
    @Scheduled(fixedDelayString = "${notification.webhook.retry-delay-ms:5000}")
    @Transactional
    public void retryPending() {
        for (WebhookDeliveryRepository.Delivery delivery : deliveryRepository.findDue(20)) {
            String secret = merchantWebhookRepository.findActive(delivery.merchantId())
                    .map(MerchantWebhookRepository.MerchantWebhook::webhookSecret)
                    .orElse(defaultSecret);
            deliverOne(delivery, secret);
        }
    }

    private void deliverOne(WebhookDeliveryRepository.Delivery delivery, String secret) {
        long ts = Instant.now().getEpochSecond();
        String signature = WebhookSigner.sign(secret, ts, delivery.payloadJson());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Webhook-Signature", signature);
            headers.add("X-Webhook-Id", String.valueOf(delivery.id()));
            headers.add("X-Payment-Id", delivery.paymentId().toString());
            headers.add("X-Event-Type", delivery.eventType());

            ResponseEntity<Void> response = restTemplate.postForEntity(
                    delivery.destinationUrl(),
                    new HttpEntity<>(delivery.payloadJson(), headers),
                    Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                deliveryRepository.markDelivered(delivery.id(), signature);
                log.info("Webhook delivered id={} payment={} event={}",
                        delivery.id(), delivery.paymentId(), delivery.eventType());
                return;
            }
            scheduleRetryOrDead(delivery, "HTTP " + response.getStatusCode().value());
        } catch (Exception e) {
            scheduleRetryOrDead(delivery, e.getMessage());
        }
    }

    private void scheduleRetryOrDead(WebhookDeliveryRepository.Delivery delivery, String error) {
        int attempts = delivery.attempts() + 1;
        if (attempts >= delivery.maxAttempts()) {
            deliveryRepository.markDead(delivery.id(), error);
            log.error("Webhook DEAD id={} payment={} error={}", delivery.id(), delivery.paymentId(), error);
            return;
        }
        long delaySec = Math.min(900, 5L * (1L << Math.min(attempts - 1, 8)));
        Instant next = Instant.now().plus(Duration.ofSeconds(delaySec));
        deliveryRepository.markRetry(delivery.id(), attempts, next, error);
        log.warn("Webhook retry id={} attempt={} next={} error={}",
                delivery.id(), attempts, next, error);
    }

    private UUID resolveMerchantId(PaymentEvent event) {
        Object raw = event.getPayload() != null ? event.getPayload().get("merchantId") : null;
        if (raw == null) {
            throw new IllegalArgumentException("merchantId missing in event payload");
        }
        return UUID.fromString(raw.toString());
    }
}
