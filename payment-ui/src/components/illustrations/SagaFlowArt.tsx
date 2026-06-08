import { PaymentStatus } from '../../types';

interface SagaFlowArtProps {
  status: PaymentStatus | null;
}

const AGENTS = ['Authorized', 'Captured', 'Settled'] as const;

function agentTone(status: PaymentStatus | null, index: number): string {
  if (!status || status === 'FAILED' || status === 'REFUNDED') return '0.2';
  const order = ['INITIATED', 'AUTHORIZED', 'CAPTURED', 'SETTLED'];
  const current = order.indexOf(status);
  if (current < 0) return '0.2';
  const agentStep = index + 1;
  if (agentStep < current) return '1';
  if (agentStep === current) return '0.85';
  return '0.2';
}

/** Flowchart wireframe — main task, agents, merged result. */
export function SagaFlowArt({ status }: SagaFlowArtProps) {
  const mergedActive = status === 'SETTLED';
  const mergedFail = status === 'FAILED' || status === 'REFUNDED';

  return (
    <svg className="bento-art__svg" viewBox="0 0 320 200" fill="none" aria-hidden>
      <rect x="110" y="8" width="100" height="28" rx="8" stroke="currentColor" strokeWidth="1.2" opacity="0.35" />
      <text x="160" y="26" textAnchor="middle" fill="currentColor" fontSize="8" fontWeight="600" opacity="0.5">
        Main task
      </text>

      <path d="M160 36v14" stroke="currentColor" strokeWidth="1.2" opacity="0.2" />
      <path d="M60 50h200" stroke="currentColor" strokeWidth="1.2" opacity="0.15" />
      <path d="M60 50v10M160 50v10M260 50v10" stroke="currentColor" strokeWidth="1.2" opacity="0.2" />

      {AGENTS.map((label, i) => {
        const x = 24 + i * 108;
        const tone = agentTone(status, i);
        return (
          <g key={label} opacity={tone}>
            <rect x={x} y="60" width="72" height="24" rx="7" stroke="currentColor" strokeWidth="1.2" />
            <text x={x + 36} y="75" textAnchor="middle" fill="currentColor" fontSize="7" fontWeight="600">
              Subtask
            </text>
            <path d={`M${x + 36} 84v10`} stroke="currentColor" strokeWidth="1.2" />
            <rect x={x + 6} y="94" width="60" height="26" rx="7" stroke="currentColor" strokeWidth="1.2" />
            <text x={x + 36} y="110" textAnchor="middle" fill="currentColor" fontSize="7" fontWeight="600">
              Agent
            </text>
            <path d={`M${x + 36} 120v8`} stroke="currentColor" strokeWidth="1.2" />
          </g>
        );
      })}

      <path d="M60 128h200" stroke="currentColor" strokeWidth="1.2" opacity="0.15" />
      <path d="M160 128v10" stroke="currentColor" strokeWidth="1.2" opacity="0.2" />

      <rect
        x="70"
        y="138"
        width="180"
        height="28"
        rx="8"
        fill={mergedFail ? '#fee' : mergedActive ? 'var(--orange)' : 'transparent'}
        stroke={mergedFail ? '#c0392b' : mergedActive ? 'var(--orange)' : 'currentColor'}
        strokeWidth="1.2"
        opacity={mergedActive || mergedFail ? 1 : 0.25}
      />
      <text
        x="160"
        y="156"
        textAnchor="middle"
        fill={mergedActive ? '#fff' : mergedFail ? '#c0392b' : 'currentColor'}
        fontSize="8"
        fontWeight="700"
        opacity={mergedActive || mergedFail ? 1 : 0.45}
      >
        Merged result
      </text>
    </svg>
  );
}
