import { PaymentStatus, SAGA_STEPS } from '../types';

interface SagaTimelineProps {
  status: PaymentStatus | null;
  polling: boolean;
}

function stepIndex(status: PaymentStatus): number {
  if (status === 'FAILED' || status === 'REFUNDED') return -1;
  return SAGA_STEPS.findIndex((s) => s.status === status);
}

function isSuccessTerminal(status: PaymentStatus): boolean {
  return status === 'SETTLED';
}

/** Gray bento tile — single-row saga pipeline with even spacing. */
export function SagaTimeline({ status, polling }: SagaTimelineProps) {
  const current = status ? stepIndex(status) : -1;
  const isTerminalFail = status === 'FAILED' || status === 'REFUNDED';
  const isComplete = status != null && isSuccessTerminal(status);

  return (
    <section className="bento bento--gray">
      <div className="pipeline__heading">
        <div>
          <span className="bento__eyebrow">Parallel Agents</span>
          <h2 className="bento__title">Move faster</h2>
          <p className="bento__desc">
            Authorization, capture and settlement run as independent saga steps over Kafka.
          </p>
        </div>
        {polling && <span className="pipeline__live">Live</span>}
      </div>

      <div className="pipeline" aria-label="Saga pipeline">
        <div className="pipeline__row">
          {SAGA_STEPS.map((step, idx) => {
            const done = status != null && idx <= current && !isTerminalFail;
            const active = status != null && idx === current && !isTerminalFail && !isComplete;

            return (
              <div key={step.status} className="pipeline__step">
                {idx > 0 && <span className="pipeline__arrow" aria-hidden />}
                <div
                  className={[
                    'pipeline__node',
                    done ? 'pipeline__node--done' : '',
                    active ? 'pipeline__node--active' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                >
                  <span className="pipeline__node-label">{step.label}</span>
                  <span className="pipeline__node-desc">{step.desc}</span>
                </div>
              </div>
            );
          })}
        </div>

        {isTerminalFail && status && (
          <div className="pipeline__terminal">{status}</div>
        )}
      </div>
    </section>
  );
}
