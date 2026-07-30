/**
 * Timeline live degli step saga.
 */
import { PaymentStatus } from '../types';
import { SagaFlowArt } from './illustrations/SagaFlowArt';

interface SagaTimelineProps {
  status: PaymentStatus | null;
  polling: boolean;
}

const STEPS: { id: PaymentStatus; label: string }[] = [
  { id: 'INITIATED', label: 'Initiated' },
  { id: 'AUTHORIZED', label: 'Authorized' },
  { id: 'CAPTURED', label: 'Captured' },
  { id: 'SETTLED', label: 'Settled' },
];

function stepIndex(status: PaymentStatus | null): number {
  if (!status) return -1;
  if (status === 'FAILED' || status === 'REFUNDED') return -2;
  return STEPS.findIndex((s) => s.id === status);
}

export function SagaTimeline({ status, polling }: SagaTimelineProps) {
  const active = stepIndex(status);

  return (
    <section className="panel panel--ink" aria-label="Saga pipeline status">
      <header className="panel__head panel__head--row">
        <div>
          <p className="panel__eyebrow panel__eyebrow--on-ink">Saga</p>
          <h2 className="panel__title panel__title--on-ink">Live pipeline</h2>
        </div>
        {polling && (
          <span className="live-dot" role="status" aria-live="polite" aria-label="Polling for updates">
            Live
          </span>
        )}
      </header>

      <ol className="saga-steps" aria-label={status ? `Payment status: ${status}` : 'Waiting for payment'}>
        {STEPS.map((step, i) => {
          const done = active >= i;
          const current = active === i;
          return (
            <li
              key={step.id}
              className={[
                'saga-steps__item',
                done ? 'saga-steps__item--done' : '',
                current ? 'saga-steps__item--current' : '',
              ]
                .filter(Boolean)
                .join(' ')}
            >
              <span className="saga-steps__index mono">{String(i + 1).padStart(2, '0')}</span>
              <span className="saga-steps__label">{step.label}</span>
            </li>
          );
        })}
      </ol>

      {(status === 'FAILED' || status === 'REFUNDED') && (
        <p className="saga-terminal" role="status">
          Terminal: <strong>{status}</strong>
        </p>
      )}

      <div className="panel__art" aria-hidden="true">
        <SagaFlowArt status={status} />
      </div>
    </section>
  );
}
