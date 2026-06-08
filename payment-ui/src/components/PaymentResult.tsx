import { PaymentResponse } from '../types';
import { ShipArt } from './illustrations/ShipArt';
import { TogetherArt } from './illustrations/TogetherArt';

interface PaymentResultProps {
  payment: PaymentResponse | null;
  replayed: boolean;
}

/** Dark + orange bento tiles — wireframe art, status and JSON response. */
export function PaymentResult({ payment, replayed }: PaymentResultProps) {
  return (
    <>
      <section className="bento bento--dark">
        <span className="bento__eyebrow bento__eyebrow--light">Response</span>
        <h2 className="bento__title bento__title--light">Ship anything</h2>

        <div className="bento-art bento-art--light">
          <ShipArt />
        </div>

        <div className="code-panel">
          {payment ? (
            <pre>{JSON.stringify(payment, null, 2)}</pre>
          ) : (
            <p className="code-panel__empty">Submit a payment to see the JSON response here.</p>
          )}
        </div>

        <p className="bento__desc bento__desc--light bento__desc--footer">
          Raw API payload from the payment-service aggregate.
        </p>
      </section>

      <section className="bento bento--orange">
        <span className="bento__eyebrow bento__eyebrow--light">Status</span>
        <h2 className="bento__title bento__title--light">Build together</h2>

        <div className="bento-art bento-art--light">
          <TogetherArt />
        </div>

        {payment ? (
          <div className="status-card">
            <div className="status-card__row">
              <span className="status-card__label">State</span>
              <span className={`status-chip status-chip--${payment.status.toLowerCase()}`}>
                {payment.status}
              </span>
            </div>
            {replayed && <span className="status-card__replay">Idempotent replay</span>}
            <div className="status-card__row">
              <span className="status-card__label">Amount</span>
              <span className="status-card__value">
                {payment.amount} {payment.currency}
              </span>
            </div>
            <div className="status-card__row">
              <span className="status-card__label">Payment ID</span>
              <span className="status-card__value mono" title={payment.id}>
                {payment.id.slice(0, 18)}…
              </span>
            </div>
          </div>
        ) : (
          <p className="bento__desc bento__desc--light bento__desc--footer">
            Payment status and saga outcome appear here after submission.
          </p>
        )}
      </section>
    </>
  );
}
