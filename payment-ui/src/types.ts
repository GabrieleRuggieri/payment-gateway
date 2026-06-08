/** Mirrors backend PaymentStatus enum. */
export type PaymentStatus =
  | 'INITIATED'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SETTLED'
  | 'FAILED'
  | 'REFUNDED';

/** API response from POST/GET /api/v1/payments. */
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

/** Ordered saga milestones for the timeline UI. */
export const SAGA_STEPS: { status: PaymentStatus; label: string; desc: string }[] = [
  { status: 'INITIATED', label: 'Initiated', desc: 'Payment created & outbox event queued' },
  { status: 'AUTHORIZED', label: 'Authorized', desc: 'Funds reserved with processor' },
  { status: 'CAPTURED', label: 'Captured', desc: 'Amount confirmed for settlement' },
  { status: 'SETTLED', label: 'Settled', desc: 'Funds transferred to merchant' },
];

export const TERMINAL_STATUSES: PaymentStatus[] = ['SETTLED', 'FAILED', 'REFUNDED'];
