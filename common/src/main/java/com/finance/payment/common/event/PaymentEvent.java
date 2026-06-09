package com.finance.payment.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Involucro Kafka per gli eventi della saga sul topic {@code payment.events}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent {

    private String eventType;
    private UUID paymentId;
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    @Builder.Default
    private Instant occurredAt = Instant.now();

    /** Crea un evento con timestamp corrente e payload normalizzato. */
    public static PaymentEvent of(PaymentEventType type, UUID paymentId, Map<String, Object> payload) {
        return PaymentEvent.builder()
                .eventType(type.wireName())
                .paymentId(paymentId)
                .payload(payload != null ? payload : Map.of())
                .occurredAt(Instant.now())
                .build();
    }
}
