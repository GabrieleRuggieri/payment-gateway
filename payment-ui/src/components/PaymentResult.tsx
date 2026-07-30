/**
 * Esito pagamento: stato + payload JSON.
 */
import { PaymentResponse } from '../types';

interface PaymentResultProps {
  payment: PaymentResponse | null;
  replayed: boolean;
}

export function PaymentResult({ payment, replayed }: PaymentResultProps) {
  return (
    <section className="panel panel--wide" aria-labelledby="result-title">
      <header className="panel__head panel__head--row">
        <div>
          <p className="panel__eyebrow">Result</p>
          <h2 id="result-title" className="panel__title">
            Payment state
          </h2>
        </div>
        {payment && (
          <span className={`status-chip status-chip--${payment.status.toLowerCase()}`}>
            {payment.status}
          </span>
        )}
      </header>

      {payment ? (
        <div className="result-grid">
          <dl className="result-meta">
            <div>
              <dt>Amount</dt>
              <dd className="mono">
                {payment.amount} {payment.currency}
              </dd>
            </div>
            <div>
              <dt>Payment ID</dt>
              <dd className="mono" title={payment.id}>
                {payment.id}
              </dd>
            </div>
            {replayed && (
              <div>
                <dt>Idempotency</dt>
                <dd>Replayed response</dd>
              </div>
            )}
          </dl>
          <pre className="result-json mono">{JSON.stringify(payment, null, 2)}</pre>
        </div>
      ) : (
        <p className="panel__empty">Submit a payment to see status and JSON here.</p>
      )}
    </section>
  );
}
