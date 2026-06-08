import { FormEvent, useState } from 'react';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

type PaymentStatus =
  | 'INITIATED'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SETTLED'
  | 'FAILED'
  | 'REFUNDED';

interface PaymentResponse {
  id: string;
  merchantId: string;
  amount: string;
  currency: string;
  status: PaymentStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

function newIdempotencyKey() {
  return crypto.randomUUID();
}

function statusClass(status: PaymentStatus) {
  return `status status-${status.toLowerCase()}`;
}

export default function App() {
  const [merchantId, setMerchantId] = useState('550e8400-e29b-41d4-a716-446655440000');
  const [amount, setAmount] = useState('99.99');
  const [currency, setCurrency] = useState('EUR');
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<PaymentResponse | null>(null);
  const [replayed, setReplayed] = useState(false);

  async function submitPayment(reuseKey: boolean) {
    setLoading(true);
    setError(null);
    setReplayed(false);

    const key = reuseKey ? idempotencyKey : newIdempotencyKey();
    if (!reuseKey) {
      setIdempotencyKey(key);
    }

    try {
      const res = await fetch(`${API_BASE}/api/v1/payments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': key,
        },
        body: JSON.stringify({
          merchantId,
          amount,
          currency,
          description: 'Demo payment from payment-ui',
        }),
      });

      const body = await res.json();
      if (!res.ok) {
        throw new Error(body.message ?? `HTTP ${res.status}`);
      }

      setResponse(body as PaymentResponse);
      setReplayed(res.headers.get('Idempotent-Replayed') === 'true');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    void submitPayment(false);
  }

  return (
    <div className="app">
      <header>
        <h1>Payment Gateway Demo</h1>
        <p>
          Small UI to exercise idempotency and payment creation. Not a production frontend.
        </p>
      </header>

      <form className="card" onSubmit={onSubmit}>
        <label htmlFor="merchantId">Merchant ID</label>
        <input
          id="merchantId"
          value={merchantId}
          onChange={(e) => setMerchantId(e.target.value)}
          required
        />

        <div className="row">
          <div>
            <label htmlFor="amount">Amount</label>
            <input
              id="amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </div>
          <div>
            <label htmlFor="currency">Currency</label>
            <input
              id="currency"
              value={currency}
              onChange={(e) => setCurrency(e.target.value.toUpperCase())}
              maxLength={3}
              required
            />
          </div>
        </div>

        <label htmlFor="idempotencyKey">Idempotency Key</label>
        <input
          id="idempotencyKey"
          value={idempotencyKey}
          onChange={(e) => setIdempotencyKey(e.target.value)}
          required
        />

        <div className="actions">
          <button type="submit" className="primary" disabled={loading}>
            {loading ? 'Submitting…' : 'Create payment'}
          </button>
          <button
            type="button"
            className="secondary"
            disabled={loading}
            onClick={() => void submitPayment(true)}
          >
            Retry same key
          </button>
          <button
            type="button"
            className="secondary"
            disabled={loading}
            onClick={() => setIdempotencyKey(newIdempotencyKey())}
          >
            New key
          </button>
        </div>

        {error && <div className="error">{error}</div>}
      </form>

      {response && (
        <section className="card">
          <h2>
            Last response
            {replayed && <span className="replay-badge">Idempotent replay</span>}
          </h2>
          <p>
            Status:{' '}
            <span className={statusClass(response.status)}>{response.status}</span>
          </p>
          <p>Payment ID: {response.id}</p>
          <pre>{JSON.stringify(response, null, 2)}</pre>
        </section>
      )}
    </div>
  );
}
