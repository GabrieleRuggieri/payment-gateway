package com.finance.payment.common.kafka;

/**
 * Parametri Kafka condivisi per carichi saga/outbox.
 * I bootstrap server dipendono dal profilo (localhost vs listener interno Docker).
 */
public final class KafkaDefaults {

    /** Host locale → porta mappata Docker (listener PLAINTEXT_HOST). */
    public static final String BOOTSTRAP_LOCAL = "localhost:9092";

    /** Comunicazione container-to-container sulla rete Docker (listener PLAINTEXT interno). */
    public static final String BOOTSTRAP_DOCKER = "kafka:29092";

    public static final int PAYMENT_EVENTS_PARTITIONS = 6;
    public static final int DLT_PARTITIONS = 3;
    public static final short REPLICATION_FACTOR_LOCAL = 1;

    public static final String PRODUCER_ACKS = "all";
    public static final int PRODUCER_MAX_IN_FLIGHT = 5;

    private KafkaDefaults() {
    }
}
