package com.finance.payment.config;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Serializza gli involucri {@link PaymentEvent} per il relay outbox transazionale.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventMapper {

    private final ObjectMapper objectMapper;

    public String toJson(PaymentEvent event) {
        return objectMapper.writeValueAsString(event);
    }

    public PaymentEvent fromJson(String json) {
        return objectMapper.readValue(json, PaymentEvent.class);
    }

    public PaymentEvent fromOutbox(String eventType, UUID paymentId, Map<String, Object> payload) {
        PaymentEventType type = PaymentEventType.fromWireName(eventType);
        return PaymentEvent.of(type, paymentId, payload);
    }

    public String outboxToJson(String eventType, UUID paymentId, Map<String, Object> payload) {
        return toJson(fromOutbox(eventType, paymentId, payload));
    }

    /**
     * Se {@code payload} è già un envelope {@link PaymentEvent} (chiave {@code eventType}),
     * lo serializza così com'è; altrimenti lo wrappa.
     */
    public String outboxRowToJson(String eventType, UUID paymentId, Map<String, Object> payload) {
        if (payload != null && payload.containsKey("eventType")) {
            return objectMapper.writeValueAsString(payload);
        }
        return outboxToJson(eventType, paymentId, payload);
    }

    /** Mappa envelope PaymentEvent da salvare nella colonna jsonb outbox (CDC-friendly). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toOutboxEnvelope(String eventType, UUID paymentId, Map<String, Object> payload) {
        PaymentEvent event = fromOutbox(eventType, paymentId, payload);
        return objectMapper.convertValue(event, Map.class);
    }
}
