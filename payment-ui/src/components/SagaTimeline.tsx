import { PaymentStatus } from '../types';
import { SagaFlowArt } from './illustrations/SagaFlowArt';

interface SagaTimelineProps {
  status: PaymentStatus | null;
  polling: boolean;
}

/** Gray bento tile — wireframe saga flowchart with live state highlights. */
export function SagaTimeline({ status, polling }: SagaTimelineProps) {
  return (
    <section className="bento bento--gray" aria-label="Saga pipeline status">
      <div className="pipeline__heading">
        <div>
          <span className="bento__eyebrow">Parallel Agents</span>
          <h2 className="bento__title">Move faster</h2>
        </div>
        {polling && (
          <span className="pipeline__live" role="status" aria-live="polite" aria-label="Polling for updates">
            Live
          </span>
        )}
      </div>

      <div
        className="bento-art bento-art--dark"
        aria-live="polite"
        aria-atomic="true"
        aria-label={status ? `Payment status: ${status}` : 'Waiting for payment'}
      >
        <SagaFlowArt status={status} />
      </div>

      <p className="bento__desc bento__desc--footer">
        Authorization, capture and settlement run as independent saga steps over Kafka.
      </p>
    </section>
  );
}
