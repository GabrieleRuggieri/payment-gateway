/**
 * Sezione hero con composer importo/valuta e pill di esempio.
 */

/** Proprietà del composer hero (importo, valuta, submit). */
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

/** Hero centrato — titolo, composer arrotondato e pill di esempio. */
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
      <p className="hero__kicker">Payment gateway demo</p>
      <h1 className="hero__title">What payment will you process?</h1>
      <p className="hero__subtitle">
        Create a payment, follow the saga to settlement, and retry with the same idempotency key.
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
          aria-label={loading ? 'Creating payment…' : 'Create payment'}
          aria-busy={loading}
        >
          {loading ? <span className="composer__spinner" aria-hidden="true" /> : '→'}
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
