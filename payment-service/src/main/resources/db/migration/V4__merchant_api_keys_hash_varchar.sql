-- Hibernate maps api_key_hash as VARCHAR(64); V3 used CHAR(64) (PostgreSQL bpchar).
ALTER TABLE merchant_api_keys
    ALTER COLUMN api_key_hash TYPE VARCHAR(64);
