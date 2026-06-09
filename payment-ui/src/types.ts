/**
 * Tipi condivisi e costanti per stati pagamento e timeline saga.
 * Allineati all'enum PaymentStatus del backend.
 */

/** Stati possibili di un pagamento nel ciclo di vita della saga. */
export type PaymentStatus =
  | 'INITIATED'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SETTLED'
  | 'FAILED'
  | 'REFUNDED';

/** Risposta API da POST/GET /api/v1/payments. */
export interface PaymentResponse {
  id: string;
  merchantId: string;
  amount: string;
  currency: string;
  status: PaymentStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

/** Milestone ordinate della saga per la timeline UI. */
export const SAGA_STEPS: { status: PaymentStatus; label: string; desc: string }[] = [
  { status: 'INITIATED', label: 'Initiated', desc: 'Payment created & outbox event queued' },
  { status: 'AUTHORIZED', label: 'Authorized', desc: 'Funds reserved with processor' },
  { status: 'CAPTURED', label: 'Captured', desc: 'Amount confirmed for settlement' },
  { status: 'SETTLED', label: 'Settled', desc: 'Funds transferred to merchant' },
];

/** Stati terminali oltre i quali il polling può fermarsi. */
export const TERMINAL_STATUSES: PaymentStatus[] = ['SETTLED', 'FAILED', 'REFUNDED'];
