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

/** Centered hero — large headline, rounded composer, example pills. */
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
      <p className="hero__kicker">Creativity runs on payments</p>
      <h1 className="hero__title">What will you build?</h1>
      <p className="hero__subtitle">
        Turn a payment into a settled saga in seconds — no manual wiring needed.
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
