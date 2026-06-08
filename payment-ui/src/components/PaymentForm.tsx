import { FormEvent } from 'react';

interface PaymentFormProps {
  merchantId: string;
  amount: string;
  currency: string;
  idempotencyKey: string;
  loading: boolean;
  error: string | null;
  onMerchantIdChange: (v: string) => void;
  onAmountChange: (v: string) => void;
  onCurrencyChange: (v: string) => void;
  onIdempotencyKeyChange: (v: string) => void;
  onSubmit: () => void;
  onRetrySameKey: () => void;
  onNewKey: () => void;
}

/** Payment creation form — posts to POST /api/v1/payments with Idempotency-Key header. */
export function PaymentForm({
  merchantId,
  amount,
  currency,
  idempotencyKey,
  loading,
  error,
  onMerchantIdChange,
  onAmountChange,
  onCurrencyChange,
  onIdempotencyKeyChange,
  onSubmit,
  onRetrySameKey,
  onNewKey,
}: PaymentFormProps) {
  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit();
  }

  return (
    <section className="panel">
      <div className="panel__head">
        <h2>New payment</h2>
        <p>Send a POST with a unique idempotency key per logical attempt.</p>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="merchantId">Merchant ID</label>
          <input
            id="merchantId"
            className="input"
            value={merchantId}
            onChange={(e) => onMerchantIdChange(e.target.value)}
            placeholder="550e8400-e29b-41d4-a716-446655440000"
            required
          />
        </div>

        <div className="form__row">
          <div className="field">
            <label htmlFor="amount">Amount</label>
            <input
              id="amount"
              className="input"
              value={amount}
              onChange={(e) => onAmountChange(e.target.value)}
              placeholder="99.99"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="currency">Currency</label>
            <input
              id="currency"
              className="input input--mono"
              value={currency}
              onChange={(e) => onCurrencyChange(e.target.value.toUpperCase())}
              maxLength={3}
              placeholder="EUR"
              required
            />
          </div>
        </div>

        <div className="field">
          <label htmlFor="idempotencyKey">
            Idempotency Key
            <span className="field__hint">UUID recommended</span>
          </label>
          <input
            id="idempotencyKey"
            className="input input--mono"
            value={idempotencyKey}
            onChange={(e) => onIdempotencyKeyChange(e.target.value)}
            required
          />
        </div>

        <div className="form__actions">
          <button type="submit" className="btn btn--primary" disabled={loading}>
            {loading ? (
              <>
                <span className="spinner" aria-hidden />
                Processing…
              </>
            ) : (
              'Create payment'
            )}
          </button>
          <button
            type="button"
            className="btn btn--ghost"
            disabled={loading}
            onClick={onRetrySameKey}
          >
            Retry same key
          </button>
          <button type="button" className="btn btn--ghost" disabled={loading} onClick={onNewKey}>
            New key
          </button>
        </div>

        {error && (
          <div className="alert alert--error" role="alert">
            {error}
          </div>
        )}
      </form>
    </section>
  );
}
