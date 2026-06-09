# payment-ui

Interfaccia web di dimostrazione per il payment gateway: crea pagamenti, segue la saga fino al settlement e include una collection di test API integrata.

## Scopo

`payment-ui` è un frontend React che interagisce con `payment-service` tramite REST. Consente di:

- inviare pagamenti con chiave di idempotenza configurabile;
- osservare in tempo reale gli stati della saga (authorization, capture, settlement);
- eseguire una suite di richieste API organizzate per cartelle (happy path, errori, idempotenza, validazione, query).

Il browser non contiene mai la chiave API del merchant: le chiamate passano da un BFF (Backend-for-Frontend) che la inietta lato server.

## Stack

| Tecnologia | Ruolo |
|------------|--------|
| React 19 | UI e stato locale |
| TypeScript | Tipizzazione |
| Vite 6 | Dev server, build e proxy di sviluppo |
| Vitest | Test unitari |
| nginx (produzione) | Hosting statico e proxy BFF verso `payment-service` |

## Sviluppo locale

```bash
cd payment-ui
npm install
npm run dev
```

L'app è disponibile su [http://localhost:3000](http://localhost:3000).

Assicurarsi che `payment-service` sia in esecuzione su `http://localhost:8080`.

Altri script:

- `npm run build` — type-check e bundle di produzione in `dist/`
- `npm run preview` — anteprima del build locale
- `npm run test` — esecuzione test Vitest

## Docker

Il `Dockerfile` esegue una build multi-stage:

1. **build** — `npm install` e `npm run build` con `VITE_API_BASE_URL` vuoto (same-origin).
2. **runtime** — immagine `nginx:alpine` che serve i file statici e fa da BFF.

```bash
docker build -t payment-ui .
docker run -p 3000:80 -e PAYMENT_API_KEY=pgw-demo-key-32chars-minimum!! payment-ui
```

`docker-entrypoint.sh` sostituisce `${PAYMENT_API_KEY}` nel template nginx prima dell'avvio.

## BFF proxy

In sviluppo, Vite (`vite.config.ts`) inoltra `/api/*` a `http://localhost:8080` e aggiunge l'header `X-Api-Key` da `PAYMENT_API_KEY` (default demo).

In produzione, nginx (`nginx.conf.template`) fa lo stesso verso `payment-service:8080`. Il client chiama solo `/api/v1/...` sullo stesso host; la chiave non è nel bundle JavaScript.

Variabili rilevanti:

| Variabile | Contesto | Descrizione |
|-----------|----------|-------------|
| `PAYMENT_API_KEY` | Vite / Docker | Chiave iniettata dal proxy BFF |
| `VITE_API_BASE_URL` | Build | Base URL API (vuoto = same-origin `/api`) |

## Test collection

La sezione **Collection** nell'UI (`src/testCollection.ts` + `src/components/TestCollection.tsx`) definisce richieste API raggruppate in cartelle:

- **Happy path** — pagamenti che raggiungono `SETTLED`
- **Failure scenarios** — `REFUNDED` e `FAILED`
- **Exactly-once initiation** — replay con `Idempotency-Key`
- **Client errors (4xx)** — validazione e verifica BFF
- **Read operations** — `GET` pagamento esistente o inesistente

Ogni test può essere eseguito singolarmente, per cartella o in blocco. I risultati mostrano HTTP status, stato finale della saga e corpo della risposta.

Test automatici: `src/testCollection.test.ts`, `src/types.test.ts`.

## Struttura del progetto

```
payment-ui/
├── Dockerfile
├── docker-entrypoint.sh
├── nginx.conf.template
├── vite.config.ts
├── vitest.config.ts
├── index.html
└── src/
    ├── main.tsx              # Entry point React
    ├── App.tsx               # Stato globale e composizione pagina
    ├── api.ts                # Client HTTP verso /api/v1/payments
    ├── types.ts              # Tipi e costanti saga
    ├── testCollection.ts     # Definizione test API
    ├── index.css             # Stili globali
    ├── components/
    │   ├── Layout.tsx        # Navigazione e footer
    │   ├── Hero.tsx          # Composer importo/valuta
    │   ├── PaymentForm.tsx   # Merchant ID e idempotency key
    │   ├── SagaTimeline.tsx  # Timeline stati saga
    │   ├── PaymentResult.tsx # JSON e stato pagamento
    │   ├── PlatformStrip.tsx # Card infrastruttura
    │   ├── TestCollection.tsx# UI collection test
    │   └── illustrations/    # SVG decorativi wireframe
    ├── types.test.ts
    └── testCollection.test.ts
```
