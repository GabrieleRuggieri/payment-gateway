package com.finance.payment.config;

import tools.jackson.databind.ObjectMapper;
import com.finance.payment.common.event.PaymentEvent;
import com.finance.payment.common.event.PaymentEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Serializes {@link PaymentEvent} envelopes for the transactional outbox relay.
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
}
