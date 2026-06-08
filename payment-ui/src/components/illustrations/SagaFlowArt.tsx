import { PaymentStatus } from '../../types';

interface SagaFlowArtProps {
  status: PaymentStatus | null;
}

function stepLevel(status: PaymentStatus | null): number {
  if (!status || status === 'FAILED' || status === 'REFUNDED') return -1;
  const order: PaymentStatus[] = ['INITIATED', 'AUTHORIZED', 'CAPTURED', 'SETTLED'];
  return order.indexOf(status);
}

function boxTone(status: PaymentStatus | null, index: number): number {
  const level = stepLevel(status);
  if (level < 0) return 0.22;
  if (status === 'SETTLED') return 0.55;
  const agentStep = index + 1;
  if (agentStep < level) return 0.55;
  if (agentStep === level) return 1;
  return 0.22;
}

/** Minimal flowchart wireframe — matches Configure / Ship style. */
export function SagaFlowArt({ status }: SagaFlowArtProps) {
  const mergedActive = status === 'SETTLED';
  const mergedFail = status === 'FAILED' || status === 'REFUNDED';
  const subtaskXs = [36, 112, 188];

  return (
    <svg className="bento-art__svg" viewBox="0 0 280 160" fill="none" aria-hidden>
      <rect x="16" y="10" width="248" height="140" rx="16" stroke="currentColor" strokeWidth="1.2" opacity="0.12" />

      {/* Main task */}
      <rect x="100" y="22" width="80" height="22" rx="8" stroke="currentColor" strokeWidth="1.2" opacity="0.35" />
      <rect x="112" y="30" width="56" height="6" rx="3" fill="currentColor" opacity="0.15" />

      <path d="M140 44v10" stroke="currentColor" strokeWidth="1.2" opacity="0.18" />
      <path d="M52 54h176" stroke="currentColor" strokeWidth="1.2" opacity="0.14" />
      <path d="M52 54v8M140 54v8M228 54v8" stroke="currentColor" strokeWidth="1.2" opacity="0.18" />

      {/* Subtasks */}
      {subtaskXs.map((x, i) => (
        <g key={`sub-${x}`} opacity={boxTone(status, i)}>
          <rect x={x} y="62" width="56" height="20" rx="7" stroke="currentColor" strokeWidth="1.2" />
          <rect x={x + 10} y="69" width="36" height="5" rx="2.5" fill="currentColor" opacity="0.18" />
        </g>
      ))}

      <path d="M64 82v8M140 82v8M216 82v8" stroke="currentColor" strokeWidth="1.2" opacity="0.18" />

      {/* Agents */}
      {subtaskXs.map((x, i) => (
        <g key={`agent-${x}`} opacity={boxTone(status, i)}>
          <rect x={x} y="90" width="56" height="22" rx="7" stroke="currentColor" strokeWidth="1.2" />
          <rect x={x + 10} y="98" width="36" height="5" rx="2.5" fill="currentColor" opacity="0.18" />
        </g>
      ))}

      <path d="M64 112v6M140 112v6M216 112v6" stroke="currentColor" strokeWidth="1.2" opacity="0.14" />
      <path d="M64 118h152" stroke="currentColor" strokeWidth="1.2" opacity="0.12" />
      <path d="M140 118v8" stroke="currentColor" strokeWidth="1.2" opacity="0.18" />

      {/* Merged result */}
      <rect
        x="72"
        y="126"
        width="136"
        height="18"
        rx="7"
        fill={mergedFail ? '#fee' : mergedActive ? 'var(--orange)' : 'currentColor'}
        opacity={mergedActive || mergedFail ? 1 : 0.12}
        stroke={mergedFail ? '#c0392b' : mergedActive ? 'var(--orange)' : 'none'}
        strokeWidth="1"
      />
    </svg>
  );
}
