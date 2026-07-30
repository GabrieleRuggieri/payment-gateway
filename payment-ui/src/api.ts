/**
 * Client HTTP per le API di pagamento.
 * Le richieste usano same-origin in Docker/dev: /api/* con BFF che inietta X-Api-Key lato server.
 */
import { PaymentResponse, TERMINAL_STATUSES } from './types';

/** Base URL API; stringa vuota = same-origin (BFF nginx o proxy Vite). */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

/** Esito normalizzato di una chiamata fetch verso payment-service. */
export interface ApiCallResult {
  ok: boolean;
  status: number;
  headers: Headers;
  body: unknown;
  error?: string;
}

/** Parametri per POST /api/v1/payments. */
export interface CreatePaymentParams {
  merchantId: string;
  amount: string;
  currency: string;
  description?: string;
  idempotencyKey?: string;
  skipIdempotencyHeader?: boolean;
  signal?: AbortSignal;
}

/** Crea un pagamento e restituisce l'esito HTTP senza lanciare eccezioni di rete. */
export async function createPayment(params: CreatePaymentParams): Promise<ApiCallResult> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (!params.skipIdempotencyHeader && params.idempotencyKey) {
    headers['Idempotency-Key'] = params.idempotencyKey;
  }

  try {
    const res = await fetch(`${API_BASE}/api/v1/payments`, {
      method: 'POST',
      headers,
      signal: params.signal,
      body: JSON.stringify({
        merchantId: params.merchantId,
        amount: params.amount,
        currency: params.currency,
        description: params.description ?? 'Demo payment from payment-ui',
      }),
    });

    const body = await res.json().catch(() => null);
    return { ok: res.ok, status: res.status, headers: res.headers, body };
  } catch (e) {
    if (params.signal?.aborted) {
      return { ok: false, status: 0, headers: new Headers(), body: null, error: 'aborted' };
    }
    return {
      ok: false,
      status: 0,
      headers: new Headers(),
      body: null,
      error: e instanceof Error ? e.message : 'Network error',
    };
  }
}

/** Recupera un pagamento per ID. */
export async function getPayment(
  paymentId: string,
  signal?: AbortSignal,
): Promise<ApiCallResult> {
  try {
    const res = await fetch(`${API_BASE}/api/v1/payments/${paymentId}`, { signal });
    const body = await res.json().catch(() => null);
    return { ok: res.ok, status: res.status, headers: res.headers, body };
  } catch (e) {
    if (signal?.aborted) {
      return { ok: false, status: 0, headers: new Headers(), body: null, error: 'aborted' };
    }
    return {
      ok: false,
      status: 0,
      headers: new Headers(),
      body: null,
      error: e instanceof Error ? e.message : 'Network error',
    };
  }
}

/** Opzioni di polling fino a stato terminale. */
export interface PollOptions {
  maxAttempts?: number;
  intervalMs?: number;
  signal?: AbortSignal;
  onUpdate?: (payment: PaymentResponse) => void;
}

/** Effettua polling su GET fino a uno stato terminale della saga, timeout o abort. */
export async function pollUntilTerminal(
  paymentId: string,
  maxAttemptsOrOptions: number | PollOptions = 30,
  intervalMs = 1500,
): Promise<PaymentResponse | null> {
  const options: PollOptions =
    typeof maxAttemptsOrOptions === 'number'
      ? { maxAttempts: maxAttemptsOrOptions, intervalMs }
      : maxAttemptsOrOptions;

  const maxAttempts = options.maxAttempts ?? 30;
  const delay = options.intervalMs ?? 1500;
  const { signal, onUpdate } = options;

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    if (signal?.aborted) return null;

    const result = await getPayment(paymentId, signal);
    if (signal?.aborted || result.error === 'aborted') return null;
    if (!result.ok || !result.body) return null;

    const payment = result.body as PaymentResponse;
    onUpdate?.(payment);
    if (TERMINAL_STATUSES.includes(payment.status)) return payment;

    await sleep(delay, signal);
    if (signal?.aborted) return null;
  }
  return null;
}

function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve) => {
    if (signal?.aborted) {
      resolve();
      return;
    }
    const timer = setTimeout(resolve, ms);
    signal?.addEventListener(
      'abort',
      () => {
        clearTimeout(timer);
        resolve();
      },
      { once: true },
    );
  });
}

/** Genera una nuova chiave di idempotenza (UUID v4). */
export function newIdempotencyKey(): string {
  return crypto.randomUUID();
}

/** Merchant ID demo predefinito per sviluppo e test. */
export const DEFAULT_MERCHANT_ID = '550e8400-e29b-41d4-a716-446655440000';
