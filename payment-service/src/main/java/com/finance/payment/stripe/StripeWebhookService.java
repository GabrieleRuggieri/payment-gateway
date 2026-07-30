package com.finance.payment.stripe;

import com.finance.payment.domain.Payment;
import com.finance.payment.service.PaymentService;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Dispute;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Verifica e applica webhook Stripe inbound (test/live) sull'aggregato pagamento.
 * Eventi gestiti: {@code charge.dispute.created}, {@code charge.refunded}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final StripeWebhookProperties properties;
    private final PaymentService paymentService;
    private final ObjectProvider<StripeClient> stripeClient;

    @Transactional
    public void handleRawEvent(String payload, String stripeSignatureHeader) {
        if (!properties.isWebhooksEnabled()) {
            throw new IllegalStateException("Stripe webhooks are disabled");
        }
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new IllegalStateException("payment.stripe.webhook-secret is not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignatureHeader, properties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe signature", e);
        }

        log.info("Stripe webhook received type={} id={}", event.getType(), event.getId());
        switch (event.getType()) {
            case "charge.dispute.created" -> handleDisputeCreated(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            default -> log.debug("Ignoring Stripe event type {}", event.getType());
        }
    }

    private void handleDisputeCreated(Event event) {
        Dispute dispute = deserialize(event, Dispute.class).orElse(null);
        if (dispute == null) {
            log.warn("Unable to deserialize Dispute from event {}", event.getId());
            return;
        }
        Optional<UUID> paymentId = resolvePaymentId(dispute.getPaymentIntent(), dispute.getCharge());
        if (paymentId.isEmpty()) {
            log.warn("No payment found for Stripe dispute id={} charge={}", dispute.getId(), dispute.getCharge());
            return;
        }
        String reason = dispute.getReason() != null ? dispute.getReason() : "stripe_dispute";
        paymentService.handleDisputed(paymentId.get(), reason);
    }

    private void handleChargeRefunded(Event event) {
        Charge charge = deserialize(event, Charge.class).orElse(null);
        if (charge == null) {
            log.warn("Unable to deserialize Charge from event {}", event.getId());
            return;
        }
        Optional<UUID> paymentId = resolvePaymentId(charge.getPaymentIntent(), charge.getId());
        if (paymentId.isEmpty()) {
            log.warn("No payment found for Stripe refund charge={}", charge.getId());
            return;
        }
        paymentService.handleRefunded(paymentId.get());
    }

    private Optional<UUID> resolvePaymentId(String paymentIntentId, String chargeId) {
        String piId = paymentIntentId;
        if ((piId == null || piId.isBlank()) && chargeId != null) {
            piId = resolvePaymentIntentFromCharge(chargeId);
        }
        if (piId != null && !piId.isBlank()) {
            Optional<Payment> byPi = paymentService.findByProcessorPaymentIntentId(piId);
            if (byPi.isPresent()) {
                return Optional.of(byPi.get().getId());
            }
            Optional<UUID> fromMetadata = readPaymentIdFromPaymentIntent(piId);
            if (fromMetadata.isPresent()) {
                return fromMetadata;
            }
        }
        return Optional.empty();
    }

    private String resolvePaymentIntentFromCharge(String chargeId) {
        StripeClient client = stripeClient.getIfAvailable();
        if (client == null || chargeId == null || !chargeId.startsWith("ch_")) {
            return chargeId != null && chargeId.startsWith("pi_") ? chargeId : null;
        }
        try {
            Charge charge = client.charges().retrieve(chargeId);
            return charge.getPaymentIntent();
        } catch (StripeException e) {
            log.warn("Unable to retrieve Stripe charge {}: {}", chargeId, e.getMessage());
            return null;
        }
    }

    private Optional<UUID> readPaymentIdFromPaymentIntent(String piId) {
        StripeClient client = stripeClient.getIfAvailable();
        if (client == null) {
            return Optional.empty();
        }
        try {
            PaymentIntent intent = client.paymentIntents().retrieve(piId);
            if (intent.getMetadata() == null) {
                return Optional.empty();
            }
            String raw = intent.getMetadata().get("paymentId");
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(raw));
        } catch (Exception e) {
            log.warn("Unable to read paymentId metadata from PI {}: {}", piId, e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> Optional<T> deserialize(Event event, Class<T> type) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject().map(obj -> {
            if (!type.isInstance(obj)) {
                return null;
            }
            return (T) obj;
        });
    }
}
