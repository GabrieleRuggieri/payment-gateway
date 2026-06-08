-- Idempotent saga consumer deduplication (PostgreSQL — durable, ACID-safe).
CREATE TABLE saga_processed_events (
    consumer_group  VARCHAR(255) NOT NULL,
    payment_id      UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (consumer_group, payment_id, event_type)
);

CREATE INDEX idx_saga_processed_at ON saga_processed_events (processed_at);
