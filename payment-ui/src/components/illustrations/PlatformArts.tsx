/** Illustrazioni SVG per le card della sezione piattaforma. */

/** Diagramma flusso pagamento → settlement. */
export function PaymentFlowArt() {
  return (
    <svg viewBox="0 0 200 160" fill="none" className="platform-art__svg" aria-hidden>
      <circle cx="100" cy="80" r="58" stroke="var(--orange)" strokeWidth="1.2" strokeDasharray="5 4" opacity="0.55" />
      <rect x="62" y="18" width="76" height="28" rx="10" fill="#fff" stroke="currentColor" strokeWidth="1.2" opacity="0.35" />
      <text x="100" y="36" textAnchor="middle" fill="currentColor" fontSize="7" fontWeight="500" opacity="0.55">
        Create payment
      </text>
      <rect x="138" y="68" width="52" height="24" rx="8" fill="var(--orange)" />
      <text x="164" y="83" textAnchor="middle" fill="#fff" fontSize="7" fontWeight="700">
        Settle
      </text>
      <rect x="24" y="108" width="56" height="28" rx="8" fill="#fff" stroke="currentColor" strokeWidth="1.2" opacity="0.3" />
      <circle cx="38" cy="122" r="6" stroke="currentColor" strokeWidth="1" opacity="0.35" />
      <text x="52" y="125" fill="currentColor" fontSize="7" fontWeight="600" opacity="0.45">
        Saga
      </text>
    </svg>
  );
}

/** Stack infrastrutturale (DB, messaging, cache, outbox). */
export function StackArt() {
  const items = ['PostgreSQL', 'Kafka', 'Redis', 'Outbox'];
  return (
    <svg viewBox="0 0 200 160" fill="none" className="platform-art__svg" aria-hidden>
      <rect x="48" y="20" width="104" height="120" rx="12" stroke="var(--orange)" strokeWidth="1.2" opacity="0.45" />
      {items.map((label, i) => (
        <g key={label}>
          <rect x="58" y={32 + i * 26} width="84" height="20" rx="8" stroke="currentColor" strokeWidth="1.2" opacity="0.3" />
          <text x="100" y={45 + i * 26} textAnchor="middle" fill="currentColor" fontSize="7" fontWeight="600" opacity="0.5">
            {label}
          </text>
        </g>
      ))}
    </svg>
  );
}

/** Hub integrazioni API, webhook ed eventi. */
export function IntegrationsArt() {
  return (
    <svg viewBox="0 0 200 160" fill="none" className="platform-art__svg" aria-hidden>
      <rect x="78" y="58" width="44" height="44" rx="10" fill="var(--orange)" />
      <path d="M92 72l8 8-8 8M108 72l-8 8 8 8" stroke="#fff" strokeWidth="2" strokeLinecap="round" />
      {[
        { x: 24, y: 24, label: 'API' },
        { x: 148, y: 24, label: 'Hook' },
        { x: 86, y: 122, label: 'Event' },
      ].map((node) => (
        <g key={node.label}>
          <path
            d={`M100 80L${node.x + 20} ${node.y + 14}`}
            stroke="currentColor"
            strokeWidth="1"
            strokeDasharray="3 3"
            opacity="0.25"
            markerEnd="url(#arrow)"
          />
          <rect x={node.x} y={node.y} width="40" height="28" rx="8" fill="#fff" stroke="currentColor" strokeWidth="1.2" opacity="0.35" />
          <text x={node.x + 20} y={node.y + 18} textAnchor="middle" fill="currentColor" fontSize="7" fontWeight="600" opacity="0.5">
            {node.label}
          </text>
        </g>
      ))}
      <defs>
        <marker id="arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
          <path d="M0,0 L6,3 L0,6" fill="currentColor" opacity="0.3" />
        </marker>
      </defs>
    </svg>
  );
}

/** Scudo sicurezza (idempotenza, deduplicazione). */
export function SecurityArt() {
  return (
    <svg viewBox="0 0 200 160" fill="none" className="platform-art__svg platform-art__svg--light" aria-hidden>
      <path
        d="M100 28 L130 42 V72 C130 92 100 108 100 108 C100 108 70 92 70 72 V42 Z"
        stroke="#fff"
        strokeWidth="1.2"
        opacity="0.45"
      />
      <path
        d="M100 36 L122 47 V70 C122 85 100 98 100 98 C100 98 78 85 78 70 V47 Z"
        stroke="#fff"
        strokeWidth="1"
        opacity="0.3"
      />
      <path d="M92 74 L98 80 L110 66" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" opacity="0.85" />
    </svg>
  );
}
