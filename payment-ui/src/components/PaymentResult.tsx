import { PaymentResponse } from '../types';
import { SagaTimeline } from './SagaTimeline';

interface PaymentResultProps {
  payment: PaymentResponse;
  replayed: boolean;
  polling: boolean;
}

function statusClass(status: string) {
  return `chip chip--${status.toLowerCase()}`;
}

/** Displays payment outcome, saga timeline and raw JSON response. */
export function PaymentResult({ payment, replayed, polling }: PaymentResultProps) {
  return (
    <section className="panel panel--result">
      <div className="panel__head">
        <div className="panel__head-row">
          <h2>Payment result</h2>
          <div className="badges">
            <span className={statusClass(payment.status)}>{payment.status}</span>
            {replayed && <span className="badge badge--warn">Idempotent replay</span>}
          </div>
        </div>
        <p className="mono truncate" title={payment.id}>
          {payment.id}
        </p>
      </div>

      <SagaTimeline status={payment.status} polling={polling} />

      <div className="metrics">
        <div className="metric">
          <span className="metric__label">Amount</span>
          <span className="metric__value">
            {payment.amount} {payment.currency}
          </span>
        </div>
        <div className="metric">
          <span className="metric__label">Merchant</span>
          <span className="metric__value mono truncate" title={payment.merchantId}>
            {payment.merchantId.slice(0, 8)}…
          </span>
        </div>
        <div className="metric">
          <span className="metric__label">Updated</span>
          <span className="metric__value">
            {new Date(payment.updatedAt).toLocaleTimeString()}
          </span>
        </div>
      </div>

      <details className="code-block">
        <summary>Raw response</summary>
        <pre>{JSON.stringify(payment, null, 2)}</pre>
      </details>
    </section>
  );
}
