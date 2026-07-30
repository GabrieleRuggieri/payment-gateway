/**
 * Pannello workspace: merchant + idempotency (interazione, non marketing).
 */

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
    <section className="panel" aria-labelledby="configure-title">
      <header className="panel__head">
        <p className="panel__eyebrow">Configure</p>
        <h2 id="configure-title" className="panel__title">
          Merchant &amp; key
        </h2>
      </header>

      <div className="panel__body">
        <div className="field">
          <label htmlFor="merchant-id">Merchant ID</label>
          <input
            id="merchant-id"
            className="mono"
            value={merchantId}
            onChange={(e) => onMerchantIdChange(e.target.value)}
            placeholder="UUID"
            aria-label="Merchant ID"
          />
        </div>
        <div className="field">
          <label htmlFor="idempotency-key">Idempotency Key</label>
          <input
            id="idempotency-key"
            className="mono"
            value={idempotencyKey}
            onChange={(e) => onIdempotencyKeyChange(e.target.value)}
            aria-label="Idempotency Key"
          />
        </div>

        <div className="panel__actions">
          <button type="button" className="btn" disabled={loading} onClick={onRetrySameKey} aria-busy={loading}>
            {loading ? 'Processing…' : 'Retry same key'}
          </button>
          <button type="button" className="btn btn--ghost" disabled={loading} onClick={onNewKey}>
            New key
          </button>
        </div>

        {error && (
          <p className="panel__error" role="alert" aria-live="assertive">
            {error}
          </p>
        )}
      </div>
    </section>
  );
}
