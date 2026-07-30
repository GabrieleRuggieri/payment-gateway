/**
 * Layout: skip-link, nav minimale, main, footer.
 */
import { ReactNode } from 'react';

interface LayoutProps {
  children: ReactNode;
}

export function Layout({ children }: LayoutProps) {
  return (
    <div className="page">
      <a className="skip-link" href="#composer">
        Skip to payment composer
      </a>

      <header className="nav-shell">
        <div className="nav">
          <a className="nav__brand" href="/" aria-label="Payment Gateway home">
            <span className="nav__mark" aria-hidden="true" />
            <span className="nav__name">Payment Gateway</span>
          </a>

          <nav className="nav__center" aria-label="Resources">
            <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">
              API
            </a>
            <a href="http://localhost:8090" target="_blank" rel="noreferrer">
              Kafka
            </a>
            <a href="#workspace">Workspace</a>
            <a href="#collection">Collection</a>
          </nav>

          <a
            className="nav__link"
            href="http://localhost:8080/actuator/health"
            target="_blank"
            rel="noreferrer"
          >
            Health
          </a>
        </div>
      </header>

      <main id="main">{children}</main>

      <footer className="footer">
        <span>Saga · Outbox · Idempotency</span>
        <span className="footer__sep">/</span>
        <span>Stripe test · Kafka KRaft</span>
      </footer>
    </div>
  );
}
