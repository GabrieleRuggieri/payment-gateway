/**
 * Layout pagina con navigazione sticky, main e footer.
 */
import { ReactNode } from 'react';

/** Contenuto figlio da renderizzare nel main. */
interface LayoutProps {
  children: ReactNode;
}

/** Barra di navigazione superiore sticky con link a risorse esterne. */
export function Layout({ children }: LayoutProps) {
  return (
    <div className="page">
      <header className="nav-shell">
        <div className="nav">
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
            <a href="#collection">Collection</a>
          </nav>

          <div className="nav__actions">
            <a className="nav__link" href="http://localhost:8080/actuator/health" target="_blank" rel="noreferrer">
              Health
            </a>
          </div>
        </div>
      </header>

      <main>{children}</main>

      <footer className="footer">
        <span>Made for demo</span>
        <span className="footer__sep">·</span>
        <span>Spring Boot 4</span>
        <span className="footer__sep">·</span>
        <span>Kafka KRaft</span>
        <span className="footer__sep">·</span>
        <span>PostgreSQL · Redis</span>
      </footer>
    </div>
  );
}
