import { ReactNode } from 'react';

interface LayoutProps {
  children: ReactNode;
}

/** Shell: top nav, gradient backdrop, footer links to local services. */
export function Layout({ children }: LayoutProps) {
  return (
    <div className="shell">
      <div className="shell__glow shell__glow--left" aria-hidden />
      <div className="shell__glow shell__glow--right" aria-hidden />

      <header className="nav">
        <div className="nav__brand">
          <span className="nav__logo" aria-hidden>
            ◆
          </span>
          <div>
            <span className="nav__title">Payment Gateway</span>
            <span className="nav__subtitle">Saga · Outbox · Idempotency</span>
          </div>
        </div>
        <nav className="nav__links" aria-label="Local services">
          <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">
            API
          </a>
          <a href="http://localhost:8090" target="_blank" rel="noreferrer">
            Kafka UI
          </a>
        </nav>
      </header>

      <main className="main">{children}</main>

      <footer className="footer">
        <span>Demo UI — not for production</span>
        <span className="footer__dot" aria-hidden>
          ·
        </span>
        <span>Spring Boot 4 · Kafka KRaft · PostgreSQL</span>
      </footer>
    </div>
  );
}
