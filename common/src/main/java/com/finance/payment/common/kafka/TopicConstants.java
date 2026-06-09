package com.finance.payment.common.kafka;

/** Costanti di topic e header Kafka condivise tra i servizi. */
public final class TopicConstants {

    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String PAYMENT_EVENTS_DLT = "payment.events.DLT";

    public static final String HEADER_EVENT_TYPE = "event-type";
    public static final String HEADER_PAYMENT_ID = "payment-id";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String HEADER_IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private TopicConstants() {
    }
}
