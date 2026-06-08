/**
 * Payment Gateway Demo UI
 *
 * Purpose: exercise the REST API (create payment, idempotency replay, status polling).
 * Not a production frontend — extend with auth, routing and merchant dashboards as needed.
 */
import { FormEvent, useCallback, useEffect, useState } from 'react';

/** Base URL for payment-service; Vite proxy or env var in docker-compose. */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

/** Mirrors backend {@link com.finance.payment.domain.PaymentStatus}. */
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

/** Saga steps shown in the lifecycle panel (order matters). */
const SAGA_STEPS: { status: PaymentStatus; label: string }[] = [
  { status: 'INITIATED', label: 'Initiated' },
  { status: 'AUTHORIZED', label: 'Authorized' },
  { status: 'CAPTURED', label: 'Captured' },
  { status: 'SETTLED', label: 'Settled' },
];

const TERMINAL_STATUSES: PaymentStatus[] = ['SETTLED', 'FAILED', 'REFUNDED'];

/** Client-generated idempotency key (UUID v4 recommended by API docs). */
function newIdempotencyKey() {
  return crypto.randomUUID();
}

function statusClass(status: PaymentStatus) {
  return `status status-${status.toLowerCase()}`;
}

function stepIndex(status: PaymentStatus): number {
  if (status === 'FAILED' || status === 'REFUNDED') {
    return -1;
  }
  return SAGA_STEPS.findIndex((s) => s.status === status);
}

export default function App() {
  const [merchantId, setMerchantId] = useState('550e8400-e29b-41d4-a716-446655440000');
  const [amount, setAmount] = useState('99.99');
  const [currency, setCurrency] = useState('EUR');
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey);
  const [loading, setLoading] = useState(false);
  const [polling, setPolling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<PaymentResponse | null>(null);
  const [replayed, setReplayed] = useState(false);

  /** Poll GET /api/v1/payments/{id} until saga reaches a terminal state. */
  const pollPaymentStatus = useCallback(async (paymentId: string) => {
    setPolling(true);
    try {
      for (let attempt = 0; attempt < 30; attempt++) {
        const res = await fetch(`${API_BASE}/api/v1/payments/${paymentId}`);
        if (!res.ok) {
          throw new Error(`Poll failed: HTTP ${res.status}`);
        }
        const body = (await res.json()) as PaymentResponse;
        setResponse(body);

        if (TERMINAL_STATUSES.includes(body.status)) {
          return;
        }
        await new Promise((r) => setTimeout(r, 1500));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Polling failed');
    } finally {
      setPolling(false);
    }
  }, []);

  /** Stop polling when component unmounts or payment id changes. */
  useEffect(() => {
    if (!response?.id || TERMINAL_STATUSES.includes(response.status)) {
      return;
    }
    void pollPaymentStatus(response.id);
    // Poll once per payment id — pollPaymentStatus loops internally until terminal.
  }, [response?.id, pollPaymentStatus]);

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

  const currentStep = response ? stepIndex(response.status) : -1;

  return (
    <div className="app">
      <header>
        <h1>Payment Gateway Demo</h1>
        <p>
          Create payments, test idempotency, and watch the saga progress via polling.
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
            Payment status
            {replayed && <span className="replay-badge">Idempotent replay</span>}
            {polling && <span className="polling-badge">Polling…</span>}
          </h2>

          <p>
            Current:{' '}
            <span className={statusClass(response.status)}>{response.status}</span>
          </p>
          <p>Payment ID: {response.id}</p>

          <div className="saga-steps" aria-label="Saga lifecycle">
            {SAGA_STEPS.map((step, idx) => (
              <div
                key={step.status}
                className={`saga-step ${
                  idx <= currentStep ? 'done' : idx === currentStep + 1 ? 'active' : ''
                }`}
              >
                <span className="saga-dot" />
                <span>{step.label}</span>
              </div>
            ))}
            {(response.status === 'FAILED' || response.status === 'REFUNDED') && (
              <div className={`saga-step terminal ${response.status.toLowerCase()}`}>
                <span className="saga-dot" />
                <span>{response.status}</span>
              </div>
            )}
          </div>

          <pre>{JSON.stringify(response, null, 2)}</pre>
        </section>
      )}
    </div>
  );
}
