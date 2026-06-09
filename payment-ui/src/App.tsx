/**
 * Componente radice: gestisce form pagamento, polling saga e integrazione test collection.
 */
import { useCallback, useEffect, useState } from 'react';
import { createPayment, getPayment, newIdempotencyKey, pollUntilTerminal } from './api';
import { Hero } from './components/Hero';
import { Layout } from './components/Layout';
import { PaymentForm } from './components/PaymentForm';
import { PaymentResult } from './components/PaymentResult';
import { SagaTimeline } from './components/SagaTimeline';
import { TestCollection } from './components/TestCollection';
import { PlatformStrip } from './components/PlatformStrip';
import { PaymentResponse, TERMINAL_STATUSES } from './types';
import './index.css';

/** Pagina principale con composer, bento grid e collection API. */
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
      const final = await pollUntilTerminal(paymentId);
      if (final) setResponse(final);
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
      const result = await createPayment({
        merchantId,
        amount,
        currency,
        idempotencyKey: key,
      });

      if (!result.ok) {
        const body = result.body as { message?: string } | null;
        throw new Error(body?.message ?? `HTTP ${result.status}`);
      }

      setResponse(result.body as PaymentResponse);
      setReplayed(result.headers.get('Idempotent-Replayed') === 'true');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  const loadPaymentById = useCallback(async (paymentId: string) => {
    const result = await getPayment(paymentId);
    if (result.ok && result.body) {
      setResponse(result.body as PaymentResponse);
      if (!TERMINAL_STATUSES.includes((result.body as PaymentResponse).status)) {
        void pollPaymentStatus(paymentId);
      }
    }
  }, [pollPaymentStatus]);

  return (
    <Layout>
      <Hero
        amount={amount}
        currency={currency}
        loading={loading}
        onAmountChange={setAmount}
        onCurrencyChange={setCurrency}
        onSubmit={() => void submitPayment(false)}
        onExample={(a, c) => {
          setAmount(a);
          setCurrency(c);
        }}
      />

      <div className="bento-grid">
        <PaymentForm
          merchantId={merchantId}
          idempotencyKey={idempotencyKey}
          loading={loading}
          error={error}
          onMerchantIdChange={setMerchantId}
          onIdempotencyKeyChange={setIdempotencyKey}
          onRetrySameKey={() => void submitPayment(true)}
          onNewKey={() => setIdempotencyKey(newIdempotencyKey())}
        />

        <SagaTimeline status={response?.status ?? null} polling={polling} />

        <PaymentResult payment={response} replayed={replayed} />
      </div>

      <PlatformStrip />
      <TestCollection merchantId={merchantId} onPaymentResult={loadPaymentById} />
    </Layout>
  );
}
