package com.finance.payment.common.kafka;

/**
 * Shared Kafka tuning for saga/outbox workloads.
 * Bootstrap servers are profile-specific (localhost vs docker internal listener).
 */
public final class KafkaDefaults {

    /** Host machine → Docker mapped port (PLAINTEXT_HOST listener). */
    public static final String BOOTSTRAP_LOCAL = "localhost:9092";

    /** Container-to-container on Docker network (PLAINTEXT internal listener). */
    public static final String BOOTSTRAP_DOCKER = "kafka:29092";

    public static final int PAYMENT_EVENTS_PARTITIONS = 6;
    public static final int DLT_PARTITIONS = 3;
    public static final short REPLICATION_FACTOR_LOCAL = 1;

    public static final String PRODUCER_ACKS = "all";
    public static final int PRODUCER_MAX_IN_FLIGHT = 5;

    private KafkaDefaults() {
    }
}
