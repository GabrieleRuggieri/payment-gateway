package com.finance.payment.webhook;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/** Store in memoria circolare per i webhook ricevuti (max {@value #MAX_EVENTS} eventi). */
@Component
public class WebhookEventStore {

    private static final int MAX_EVENTS = 200;

    private final Deque<StoredWebhook> events = new ConcurrentLinkedDeque<>();

    /** Aggiunge un payload webhook mantenendo la finestra di eventi più recenti. */
    public void add(String payload) {
        add(payload, null, null, null);
    }

    public void add(String payload, String signature, String webhookId, String eventType) {
        events.addFirst(new StoredWebhook(Instant.now(), payload, signature, webhookId, eventType));
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    /** Restituisce una copia immutabile degli eventi attualmente memorizzati. */
    public List<StoredWebhook> snapshot() {
        return new ArrayList<>(events);
    }

    /**
     * Webhook memorizzato con timestamp e metadati di firma.
     */
    public record StoredWebhook(
            Instant receivedAt,
            String payload,
            String signature,
            String webhookId,
            String eventType) {
    }
}
