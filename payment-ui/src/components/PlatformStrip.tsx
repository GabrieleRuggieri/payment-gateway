/**
 * Una sola sezione: il percorso della saga — niente griglia marketing a card.
 */

const STEPS = [
  {
    title: 'Authorize',
    body: 'Funds are held on the processor (Stripe PaymentIntent, manual capture).',
  },
  {
    title: 'Capture',
    body: 'The hold becomes a charge. Failure voids the authorization.',
  },
  {
    title: 'Settle',
    body: 'Ledger confirms settlement — or refunds when the acquirer path fails.',
  },
];

export function PlatformStrip() {
  return (
    <section className="flow" aria-labelledby="flow-title">
      <div className="flow__inner">
        <p className="flow__eyebrow">How money moves</p>
        <h2 id="flow-title" className="flow__title">
          Three steps. One saga.
        </h2>
        <p className="flow__desc">
          Choreographed over Kafka with an outbox — each service reacts, compensates, and never double-charges.
        </p>

        <ol className="flow__steps">
          {STEPS.map((step, i) => (
            <li key={step.title} className="flow__step">
              <span className="flow__num mono">{String(i + 1).padStart(2, '0')}</span>
              <h3 className="flow__step-title">{step.title}</h3>
              <p className="flow__step-body">{step.body}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
