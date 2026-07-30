/**
 * Componente radice: gestisce form pagamento, polling saga e integrazione test collection.
 */
import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { createPayment, getPayment, newIdempotencyKey, pollUntilTerminal } from './api';
import { Hero } from './components/Hero';
import { Layout } from './components/Layout';
import { PaymentForm } from './components/PaymentForm';
import { PaymentResult } from './components/PaymentResult';
import { SagaTimeline } from './components/SagaTimeline';
import { PlatformStrip } from './components/PlatformStrip';
import { PaymentResponse, TERMINAL_STATUSES } from './types';

const TestCollection = lazy(() =>
  import('./components/TestCollection').then((m) => ({ default: m.TestCollection })),
);

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

  const pollAbortRef = useRef<AbortController | null>(null);

  const stopPolling = useCallback(() => {
    pollAbortRef.current?.abort();
    pollAbortRef.current = null;
    setPolling(false);
  }, []);

  const pollPaymentStatus = useCallback(async (paymentId: string) => {
    pollAbortRef.current?.abort();
    const controller = new AbortController();
    pollAbortRef.current = controller;

    setPolling(true);
    setError(null);
    try {
      const final = await pollUntilTerminal(paymentId, {
        signal: controller.signal,
        onUpdate: (payment) => {
          if (!controller.signal.aborted) {
            setResponse(payment);
          }
        },
      });
      if (!controller.signal.aborted && final) {
        setResponse(final);
      }
    } catch (e) {
      if (!controller.signal.aborted) {
        setError(e instanceof Error ? e.message : 'Polling failed');
      }
    } finally {
      if (pollAbortRef.current === controller) {
        pollAbortRef.current = null;
        setPolling(false);
      }
    }
  }, []);

  useEffect(() => () => stopPolling(), [stopPolling]);

  // Avvia il poll solo quando cambia l'id del pagamento (non a ogni update di status).
  useEffect(() => {
    if (!response?.id || TERMINAL_STATUSES.includes(response.status)) return;
    void pollPaymentStatus(response.id);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- solo al cambio id
  }, [response?.id]);

  async function submitPayment(reuseKey: boolean) {
    stopPolling();
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
    stopPolling();
    const result = await getPayment(paymentId);
    if (result.ok && result.body) {
      const payment = result.body as PaymentResponse;
      setResponse(payment);
      if (!TERMINAL_STATUSES.includes(payment.status)) {
        void pollPaymentStatus(paymentId);
      }
    }
  }, [stopPolling, pollPaymentStatus]);

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

      <div className="workspace" id="workspace">
        <div className="workspace__grid">
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
      </div>

      <PlatformStrip />

      <Suspense
        fallback={
          <section className="test-collection" id="collection" aria-busy="true">
            <p className="test-collection__loading">Loading API collection…</p>
          </section>
        }
      >
        <TestCollection merchantId={merchantId} onPaymentResult={loadPaymentById} />
      </Suspense>
    </Layout>
  );
}
