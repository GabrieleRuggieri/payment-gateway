-- Merchant webhook endpoints and durable delivery queue.
CREATE TABLE merchant_webhooks (
    merchant_id    UUID PRIMARY KEY,
    webhook_url    TEXT NOT NULL,
    webhook_secret VARCHAR(128) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Demo merchant → webhook-receiver locale; secret noto per verificare HMAC in demo.
INSERT INTO merchant_webhooks (merchant_id, webhook_url, webhook_secret, active)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'http://webhook-receiver:8099/webhooks/payments',
    'whsec_demo_payment_gateway_local',
    TRUE
);

CREATE TABLE webhook_deliveries (
    id              BIGSERIAL PRIMARY KEY,
    merchant_id     UUID NOT NULL,
    payment_id      UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    destination_url TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 8,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_error      TEXT,
    signature       VARCHAR(128),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at    TIMESTAMPTZ,
    CONSTRAINT webhook_deliveries_status_valid CHECK (
        status IN ('PENDING', 'DELIVERED', 'DEAD')
    )
);

CREATE INDEX idx_webhook_deliveries_pending
    ON webhook_deliveries (next_attempt_at)
    WHERE status = 'PENDING';

CREATE UNIQUE INDEX idx_webhook_deliveries_dedup
    ON webhook_deliveries (payment_id, event_type);
