/** Landing hero — explains what the demo does at a glance. */
export function Hero() {
  return (
    <section className="hero">
      <h1>
        Build payments
        <span className="hero__accent"> in seconds</span>
      </h1>
      <p>
        Create a payment, watch the distributed saga complete, and verify idempotent retries —
        all against your local gateway stack.
      </p>
      <div className="hero__pills">
        <span className="pill">Outbox pattern</span>
        <span className="pill">Kafka saga</span>
        <span className="pill">Idempotency keys</span>
      </div>
    </section>
  );
}
