import { useCallback, useEffect, useState } from 'react';
import { Hero } from './components/Hero';
import { Layout } from './components/Layout';
import { PaymentForm } from './components/PaymentForm';
import { PaymentResult } from './components/PaymentResult';
import { PaymentResponse, TERMINAL_STATUSES } from './types';
import './index.css';

/** Base URL — empty string uses nginx proxy in Docker or Vite proxy locally. */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

function newIdempotencyKey() {
  return crypto.randomUUID();
}

export default function App() {
  const [merchantId, setMerchantId] = useState<string>('550e8400-e29b-41d4-a716-446655440000');
  const [amount, setAmount] = useState('99.99');
  const [currency, setCurrency] = useState('EUR');
  const [idempotencyKey, setIdempotencyKey] = useState<string>(newIdempotencyKey);
  const [loading, setLoading] = useState(false);
  const [polling, setPolling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<PaymentResponse | null>(null);
  const [replayed, setReplayed] = useState(false);

  const pollPaymentStatus = useCallback(async (paymentId: string) => {
    setPolling(true);
    try {
      for (let attempt = 0; attempt < 30; attempt++) {
        const res = await fetch(`${API_BASE}/api/v1/payments/${paymentId}`);
        if (!res.ok) throw new Error(`Poll failed: HTTP ${res.status}`);
        const body = (await res.json()) as PaymentResponse;
        setResponse(body);
        if (TERMINAL_STATUSES.includes(body.status)) return;
        await new Promise((r) => setTimeout(r, 1500));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Polling failed');
    } finally {
      setPolling(false);
    }
  }, []);

  useEffect(() => {
    if (!response?.id || TERMINAL_STATUSES.includes(response.status)) return;
    void pollPaymentStatus(response.id);
  }, [response?.id, pollPaymentStatus]);

  async function submitPayment(reuseKey: boolean) {
    setLoading(true);
    setError(null);
    setReplayed(false);

    const key = reuseKey ? idempotencyKey : newIdempotencyKey();
    if (!reuseKey) setIdempotencyKey(key);

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
      if (!res.ok) throw new Error(body.message ?? `HTTP ${res.status}`);

      setResponse(body as PaymentResponse);
      setReplayed(res.headers.get('Idempotent-Replayed') === 'true');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <Layout>
      <Hero />

      <div className="grid">
        <PaymentForm
          merchantId={merchantId}
          amount={amount}
          currency={currency}
          idempotencyKey={idempotencyKey}
          loading={loading}
          error={error}
          onMerchantIdChange={setMerchantId}
          onAmountChange={setAmount}
          onCurrencyChange={setCurrency}
          onIdempotencyKeyChange={setIdempotencyKey}
          onSubmit={() => void submitPayment(false)}
          onRetrySameKey={() => void submitPayment(true)}
          onNewKey={() => setIdempotencyKey(newIdempotencyKey())}
        />

        {response ? (
          <PaymentResult payment={response} replayed={replayed} polling={polling} />
        ) : (
          <section className="panel panel--empty">
            <div className="empty-state">
              <span className="empty-state__icon" aria-hidden>
                ◇
              </span>
              <h3>No payment yet</h3>
              <p>Submit the form to start the saga. Status updates appear here in real time.</p>
            </div>
          </section>
        )}
      </div>
    </Layout>
  );
}
