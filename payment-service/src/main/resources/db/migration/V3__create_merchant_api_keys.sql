-- Merchant API keys (SHA-256 hash stored, never the raw key).
CREATE TABLE merchant_api_keys (
    api_key_hash CHAR(64) PRIMARY KEY,
    merchant_id  UUID NOT NULL,
    label        VARCHAR(100) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_merchant_api_keys_merchant ON merchant_api_keys (merchant_id);

-- Demo key (raw value for local dev): pgw-demo-key-32chars-minimum!!
INSERT INTO merchant_api_keys (api_key_hash, merchant_id, label)
VALUES (
    '8c346384c1d79c5445018538eb8639e151050db2266f82556ac5d67369b07909',
    '550e8400-e29b-41d4-a716-446655440000',
    'demo-merchant'
);
