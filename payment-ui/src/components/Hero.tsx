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
  { label: 'Standard €49.99', amount: '49.99', currency: 'EUR' },
  { label: 'Small €9.99 test', amount: '9.99', currency: 'EUR' },
  { label: 'USD $150.00', amount: '150.00', currency: 'USD' },
];

const TAGS = ['Outbox', 'Saga', 'Idempotency', 'Kafka'];

/** Centered hero with large rounded composer — primary entry point for a payment. */
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
    <section className="hero">
      <h1 className="hero__title">What payment will you process?</h1>
      <p className="hero__subtitle">
        Create a payment, watch the distributed saga complete, and verify idempotent retries.
      </p>

      <div className="composer">
        <div className="composer__inner">
          <span className="composer__prefix">Amount</span>
          <input
            className="composer__input"
            value={amount}
            onChange={(e) => onAmountChange(e.target.value)}
            placeholder="99.99"
            aria-label="Amount"
          />
          <input
            className="composer__currency"
            value={currency}
            onChange={(e) => onCurrencyChange(e.target.value.toUpperCase())}
            maxLength={3}
            aria-label="Currency"
          />
        </div>
        <button
          type="button"
          className="composer__submit"
          onClick={onSubmit}
          disabled={loading}
          aria-label="Create payment"
        >
          {loading ? <span className="composer__spinner" /> : '→'}
        </button>
      </div>

      <div className="hero__tags">
        {TAGS.map((tag) => (
          <span key={tag} className="tag-pill">
            {tag}
          </span>
        ))}
      </div>

      <div className="examples">
        <span className="examples__label">Try an example amount</span>
        <div className="examples__pills">
          {EXAMPLES.map((ex) => (
            <button
              key={ex.label}
              type="button"
              className="example-pill"
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
