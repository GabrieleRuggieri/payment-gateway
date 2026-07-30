/**
 * Sezione hero full-bleed: brand, headline, composer — un solo composition.
 */

interface HeroProps {
  amount: string;
  currency: string;
  loading: boolean;
  onAmountChange: (v: string) => void;
  onCurrencyChange: (v: string) => void;
  onSubmit: () => void;
  onExample: (amount: string, currency: string) => void;
}

const EXAMPLES = [
  { label: '€49.99', amount: '49.99', currency: 'EUR' },
  { label: '€9.99', amount: '9.99', currency: 'EUR' },
  { label: '$150', amount: '150.00', currency: 'USD' },
];

/** Hero edge-to-edge con brand hero-level e CTA composer. */
export function Hero({
  amount,
  currency,
  loading,
  onAmountChange,
  onCurrencyChange,
  onSubmit,
  onExample,
}: HeroProps) {
  return (
    <section className="hero" id="composer">
      <div className="hero__mesh" aria-hidden="true" />
      <div className="hero__ledger" aria-hidden="true" />

      <div className="hero__inner">
        <p className="hero__brand">Payment Gateway</p>
        <h1 className="hero__title">Authorize. Capture. Settle.</h1>
        <p className="hero__subtitle">
          Run a payment through the saga — then replay with the same idempotency key.
        </p>

        <form
          className="composer"
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit();
          }}
        >
          <label className="composer__field">
            <span className="composer__label">Amount</span>
            <input
              className="composer__input mono"
              value={amount}
              onChange={(e) => onAmountChange(e.target.value)}
              placeholder="99.99"
              inputMode="decimal"
              aria-label="Amount"
            />
          </label>
          <label className="composer__field composer__field--currency">
            <span className="composer__label">CCY</span>
            <input
              className="composer__currency mono"
              value={currency}
              onChange={(e) => onCurrencyChange(e.target.value.toUpperCase())}
              maxLength={3}
              aria-label="Currency"
            />
          </label>
          <button
            type="submit"
            className="composer__submit"
            disabled={loading}
            aria-label={loading ? 'Creating payment…' : 'Create payment'}
            aria-busy={loading}
          >
            {loading ? <span className="composer__spinner" aria-hidden="true" /> : 'Run payment'}
          </button>
        </form>

        <div className="hero__examples">
          <span className="hero__examples-label">Quick amounts</span>
          {EXAMPLES.map((ex) => (
            <button
              key={ex.label}
              type="button"
              className="hero__example"
              onClick={() => onExample(ex.amount, ex.currency)}
            >
              {ex.label}
            </button>
          ))}
        </div>
      </div>
    </section>
  );
}
