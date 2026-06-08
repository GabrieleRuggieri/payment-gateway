import {
  IntegrationsArt,
  PaymentFlowArt,
  SecurityArt,
  StackArt,
} from './illustrations/PlatformArts';

const PLATFORM_CARDS = [
  {
    id: 'flow',
    eyebrow: 'Payment flow',
    title: 'Describe it. Settle it.',
    desc: 'Initiate a payment and watch the saga complete end-to-end.',
    variant: 'white' as const,
    Art: PaymentFlowArt,
  },
  {
    id: 'stack',
    eyebrow: 'Full stack infrastructure',
    title: 'Build & scale easily',
    desc: 'PostgreSQL, Kafka, Redis and outbox with zero extra wiring.',
    variant: 'gray' as const,
    Art: StackArt,
  },
  {
    id: 'integrations',
    eyebrow: 'Integrations',
    title: 'Connect to services',
    desc: 'REST API, webhooks and Kafka events for every saga step.',
    variant: 'peach' as const,
    Art: IntegrationsArt,
  },
  {
    id: 'security',
    eyebrow: 'Enterprise control',
    title: 'Secure your payments',
    desc: 'Idempotency keys, deduplication and durable outbox patterns.',
    variant: 'orange' as const,
    Art: SecurityArt,
  },
];

/** Platform strip — four illustrated cards with wireframe diagrams. */
export function PlatformStrip() {
  return (
    <section className="platform-strip">
      <div className="platform-strip__header">
        <span className="platform-strip__eyebrow">Powered by the platform</span>
        <h2 className="platform-strip__title">Build &amp; scale your apps easily</h2>
        <p className="platform-strip__desc">
          Built-in services with zero setup — persistence, messaging and deduplication from day one.
        </p>
      </div>

      <div className="platform-strip__grid">
        {PLATFORM_CARDS.map(({ id, eyebrow, title, desc, variant, Art }) => (
          <article key={id} className={`platform-card platform-card--${variant}`}>
            <span className={`platform-card__eyebrow${variant === 'orange' ? ' platform-card__eyebrow--light' : ''}`}>
              {eyebrow}
            </span>
            <h3 className={`platform-card__title${variant === 'orange' ? ' platform-card__title--light' : ''}`}>
              {title}
            </h3>
            <div className={`platform-card__art${variant === 'orange' ? ' platform-card__art--light' : ''}`}>
              <Art />
            </div>
            <p className={`platform-card__desc${variant === 'orange' ? ' platform-card__desc--light' : ''}`}>
              {desc}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}
