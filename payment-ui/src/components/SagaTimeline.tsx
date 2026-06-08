import { PaymentStatus, SAGA_STEPS } from '../types';

interface SagaTimelineProps {
  status: PaymentStatus;
  polling: boolean;
}

function stepIndex(status: PaymentStatus): number {
  if (status === 'FAILED' || status === 'REFUNDED') return -1;
  return SAGA_STEPS.findIndex((s) => s.status === status);
}

/** Visual pipeline for the payment saga — highlights current and completed steps. */
export function SagaTimeline({ status, polling }: SagaTimelineProps) {
  const current = stepIndex(status);
  const isTerminalFail = status === 'FAILED' || status === 'REFUNDED';

  return (
    <div className="timeline" aria-label="Saga lifecycle">
      <div className="timeline__header">
        <span className="timeline__label">Pipeline</span>
        {polling && (
          <span className="badge badge--pulse">
            <span className="badge__dot" aria-hidden />
            Live
          </span>
        )}
      </div>

      <ol className="timeline__steps">
        {SAGA_STEPS.map((step, idx) => {
          const done = idx <= current && !isTerminalFail;
          const active = idx === current + 1 && !isTerminalFail && polling;
          const currentStep = idx === current && !isTerminalFail;

          return (
            <li
              key={step.status}
              className={[
                'timeline__step',
                done ? 'timeline__step--done' : '',
                currentStep ? 'timeline__step--current' : '',
                active ? 'timeline__step--active' : '',
              ]
                .filter(Boolean)
                .join(' ')}
            >
              <span className="timeline__marker" aria-hidden>
                {done ? '✓' : idx + 1}
              </span>
              <div className="timeline__content">
                <span className="timeline__name">{step.label}</span>
                <span className="timeline__desc">{step.desc}</span>
              </div>
            </li>
          );
        })}

        {isTerminalFail && (
          <li className={`timeline__step timeline__step--terminal timeline__step--${status.toLowerCase()}`}>
            <span className="timeline__marker" aria-hidden>
              !
            </span>
            <div className="timeline__content">
              <span className="timeline__name">{status}</span>
              <span className="timeline__desc">Saga reached terminal state</span>
            </div>
          </li>
        )}
      </ol>
    </div>
  );
}
