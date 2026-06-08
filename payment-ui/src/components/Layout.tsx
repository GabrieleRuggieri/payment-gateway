import { ReactNode } from 'react';

interface LayoutProps {
  children: ReactNode;
}

/** Top navigation — cream background, orange brand mark, pill badge. */
export function Layout({ children }: LayoutProps) {
  return (
    <div className="page">
      <header className="nav">
        <a className="nav__brand" href="/">
          <span className="nav__logo" aria-hidden>
            <span className="nav__logo-dots" />
          </span>
          <span className="nav__name">Payment Gateway</span>
        </a>

        <nav className="nav__center" aria-label="Resources">
          <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer">
            API Docs
          </a>
          <a href="http://localhost:8090" target="_blank" rel="noreferrer">
            Kafka UI
          </a>
        </nav>

        <div className="nav__actions">
          <a className="nav__link" href="http://localhost:8080/actuator/health" target="_blank" rel="noreferrer">
            Health
          </a>
          <span className="nav__cta">Demo mode</span>
        </div>
      </header>

      <main>{children}</main>

      <footer className="footer">
        <span>Spring Boot 4</span>
        <span className="footer__sep">·</span>
        <span>Kafka KRaft</span>
        <span className="footer__sep">·</span>
        <span>PostgreSQL · Redis</span>
      </footer>
    </div>
  );
}
