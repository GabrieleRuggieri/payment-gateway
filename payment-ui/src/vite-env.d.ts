/// <reference types="vite/client" />

/** Variabili d'ambiente esposte da Vite al bundle client. */
interface ImportMetaEnv {
  /** Base URL delle API; vuoto = richieste same-origin verso /api (BFF). */
  readonly VITE_API_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
