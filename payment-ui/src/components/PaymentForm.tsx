interface PaymentFormProps {
  merchantId: string;
  idempotencyKey: string;
  loading: boolean;
  error: string | null;
  onMerchantIdChange: (v: string) => void;
  onIdempotencyKeyChange: (v: string) => void;
  onRetrySameKey: () => void;
  onNewKey: () => void;
}

/** Peach bento tile — merchant & idempotency configuration. */
export function PaymentForm({
  merchantId,
  idempotencyKey,
  loading,
  error,
  onMerchantIdChange,
  onIdempotencyKeyChange,
  onRetrySameKey,
  onNewKey,
}: PaymentFormProps) {
  return (
    <section className="bento bento--peach">
      <span className="bento__eyebrow">Configuration</span>
      <h2 className="bento__title">Configure freely</h2>
      <p className="bento__desc">
        Set merchant and idempotency key before submitting. Each key guarantees exactly-once
        initiation.
      </p>

      <div className="bento-form">
        <label className="bento-field">
          <span>Merchant ID</span>
          <input
            value={merchantId}
            onChange={(e) => onMerchantIdChange(e.target.value)}
            placeholder="UUID"
          />
        </label>
        <label className="bento-field">
          <span>Idempotency Key</span>
          <input
            className="mono"
            value={idempotencyKey}
            onChange={(e) => onIdempotencyKeyChange(e.target.value)}
          />
        </label>

        <div className="bento-form__actions">
          <button type="button" className="btn-outline" disabled={loading} onClick={onRetrySameKey}>
            Retry same key
          </button>
          <button type="button" className="btn-outline" disabled={loading} onClick={onNewKey}>
            New key
          </button>
        </div>

        {error && <p className="bento-error">{error}</p>}
      </div>
    </section>
  );
}
