import { ConfigureArt } from './illustrations/ConfigureArt';

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

/** Peach bento tile — wireframe art + merchant & idempotency configuration. */
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
      <span className="bento__eyebrow">Design Freely</span>
      <h2 className="bento__title">Configure freely</h2>

      <div className="bento-art bento-art--dark">
        <ConfigureArt />
      </div>

      <div className="bento-form">
        <div className="bento-field">
          <label htmlFor="merchant-id">Merchant ID</label>
          <input
            id="merchant-id"
            value={merchantId}
            onChange={(e) => onMerchantIdChange(e.target.value)}
            placeholder="UUID"
            aria-label="Merchant ID"
          />
        </div>
        <div className="bento-field">
          <label htmlFor="idempotency-key">Idempotency Key</label>
          <input
            id="idempotency-key"
            className="mono"
            value={idempotencyKey}
            onChange={(e) => onIdempotencyKeyChange(e.target.value)}
            aria-label="Idempotency Key"
          />
        </div>

        <div className="bento-form__actions">
          <button
            type="button"
            className="btn-outline"
            disabled={loading}
            onClick={onRetrySameKey}
            aria-busy={loading}
          >
            {loading ? 'Processing…' : 'Retry same key'}
          </button>
          <button type="button" className="btn-outline" disabled={loading} onClick={onNewKey}>
            New key
          </button>
        </div>

        {error && (
          <p className="bento-error" role="alert" aria-live="assertive">
            {error}
          </p>
        )}
      </div>

      <p className="bento__desc bento__desc--footer">
        Set merchant and idempotency key before submitting. Each key guarantees exactly-once initiation.
      </p>
    </section>
  );
}
