package com.finance.payment.webhook;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class WebhookEventStore {

    private static final int MAX_EVENTS = 200;

    private final Deque<StoredWebhook> events = new ConcurrentLinkedDeque<>();

    public void add(String payload) {
        events.addFirst(new StoredWebhook(Instant.now(), payload));
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    public List<StoredWebhook> snapshot() {
        return new ArrayList<>(events);
    }

    public record StoredWebhook(Instant receivedAt, String payload) {
    }
}
