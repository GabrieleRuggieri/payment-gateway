-- DISPUTED: chargeback / dispute Stripe (test o live).
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_valid;
ALTER TABLE payments ADD CONSTRAINT payments_status_valid CHECK (
    status IN ('INITIATED','AUTHORIZED','CAPTURED','SETTLED','FAILED','REFUNDED','DISPUTED')
);

-- Indice per correlare webhook Stripe (PaymentIntent id salvato in metadata).
CREATE INDEX IF NOT EXISTS idx_payments_processor_pi
    ON payments ((metadata->>'processorPaymentIntentId'))
    WHERE metadata ? 'processorPaymentIntentId';

-- Debezium Outbox Event Router: colonna aggregatetype (valore fisso per route).
ALTER TABLE payment_outbox
    ADD COLUMN IF NOT EXISTS aggregate_type VARCHAR(100) NOT NULL DEFAULT 'Payment';
