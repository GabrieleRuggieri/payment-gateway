import { ApiCallResult, DEFAULT_MERCHANT_ID, createPayment, getPayment, pollUntilTerminal } from './api';
import { PaymentResponse } from './types';

export type TestRunStatus = 'idle' | 'running' | 'passed' | 'failed';

export interface TestRunState {
  status: TestRunStatus;
  httpStatus?: number;
  message?: string;
  detail?: string;
}

export interface TestContext {
  merchantId: string;
  lastPaymentId: string | null;
  lastIdempotencyKey: string | null;
  replayIdempotencyKey: string | null;
}

export interface ApiTestCase {
  id: string;
  name: string;
  description: string;
  expected: string;
  run: (ctx: TestContext) => Promise<{ pass: boolean; httpStatus: number; message: string; detail?: string }>;
}

export interface TestSection {
  id: string;
  eyebrow: string;
  title: string;
  description: string;
  variant: 'peach' | 'gray' | 'dark';
  tests: ApiTestCase[];
}

function paymentFrom(result: ApiCallResult): PaymentResponse | null {
  if (!result.body || typeof result.body !== 'object') return null;
  return result.body as PaymentResponse;
}

function formatBody(result: ApiCallResult): string {
  if (result.error) return result.error;
  try {
    return JSON.stringify(result.body, null, 2);
  } catch {
    return String(result.body);
  }
}

export function createInitialTestContext(merchantId: string): TestContext {
  return {
    merchantId,
    lastPaymentId: null,
    lastIdempotencyKey: null,
    replayIdempotencyKey: null,
  };
}

export const TEST_SECTIONS: TestSection[] = [
  {
    id: 'success',
    eyebrow: 'Success paths',
    title: 'Happy path',
    description: 'Payments that complete the full saga and reach SETTLED.',
    variant: 'peach',
    tests: [
      {
        id: 'success-standard',
        name: 'Standard EUR payment',
        description: 'POST €49.99 — typical demo amount.',
        expected: 'HTTP 200 → saga completes → SETTLED',
        run: async (ctx) => {
          const key = crypto.randomUUID();
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '49.99',
            currency: 'EUR',
            idempotencyKey: key,
            description: 'Test: standard EUR payment',
          });
          if (!created.ok) {
            return { pass: false, httpStatus: created.status, message: 'Create failed', detail: formatBody(created) };
          }
          const payment = paymentFrom(created)!;
          ctx.lastPaymentId = payment.id;
          ctx.lastIdempotencyKey = key;
          const final = await pollUntilTerminal(payment.id);
          const pass = final?.status === 'SETTLED';
          return {
            pass,
            httpStatus: created.status,
            message: pass ? `SETTLED (${payment.id.slice(0, 8)}…)` : `Got ${final?.status ?? 'timeout'}`,
            detail: final ? JSON.stringify(final, null, 2) : undefined,
          };
        },
      },
      {
        id: 'success-small',
        name: 'Small amount',
        description: 'POST €9.99 — minimum realistic charge.',
        expected: 'HTTP 200 → SETTLED',
        run: async (ctx) => {
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '9.99',
            currency: 'EUR',
            idempotencyKey: crypto.randomUUID(),
          });
          if (!created.ok) {
            return { pass: false, httpStatus: created.status, message: 'Create failed', detail: formatBody(created) };
          }
          const payment = paymentFrom(created)!;
          const final = await pollUntilTerminal(payment.id);
          const pass = final?.status === 'SETTLED';
          return {
            pass,
            httpStatus: created.status,
            message: pass ? 'SETTLED' : `Got ${final?.status ?? 'timeout'}`,
          };
        },
      },
      {
        id: 'success-usd',
        name: 'USD payment',
        description: 'POST $150.00 in USD currency.',
        expected: 'HTTP 200 → SETTLED',
        run: async (ctx) => {
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '150.00',
            currency: 'USD',
            idempotencyKey: crypto.randomUUID(),
          });
          if (!created.ok) {
            return { pass: false, httpStatus: created.status, message: 'Create failed', detail: formatBody(created) };
          }
          const final = await pollUntilTerminal(paymentFrom(created)!.id);
          const pass = final?.status === 'SETTLED';
          return { pass, httpStatus: created.status, message: pass ? 'SETTLED' : `Got ${final?.status ?? 'timeout'}` };
        },
      },
      {
        id: 'success-limit',
        name: 'Processor limit (edge)',
        description: 'POST €9999.00 — highest amount the mock processor accepts.',
        expected: 'HTTP 200 → SETTLED',
        run: async (ctx) => {
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '9999.00',
            currency: 'EUR',
            idempotencyKey: crypto.randomUUID(),
          });
          if (!created.ok) {
            return { pass: false, httpStatus: created.status, message: 'Create failed', detail: formatBody(created) };
          }
          const final = await pollUntilTerminal(paymentFrom(created)!.id);
          const pass = final?.status === 'SETTLED';
          return { pass, httpStatus: created.status, message: pass ? 'SETTLED at limit' : `Got ${final?.status ?? 'timeout'}` };
        },
      },
    ],
  },
  {
    id: 'failures',
    eyebrow: 'Saga failures',
    title: 'Failure scenarios',
    description: 'Payments that fail during authorization after processor retries.',
    variant: 'gray',
    tests: [
      {
        id: 'fail-auth-limit',
        name: 'Authorization limit exceeded',
        description: 'POST €10000.00 — mock processor rejects amounts above 9999.',
        expected: 'HTTP 200 → saga fails → FAILED',
        run: async (ctx) => {
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '10000.00',
            currency: 'EUR',
            idempotencyKey: crypto.randomUUID(),
            description: 'Test: authorization limit exceeded',
          });
          if (!created.ok) {
            return { pass: false, httpStatus: created.status, message: 'Create failed', detail: formatBody(created) };
          }
          const final = await pollUntilTerminal(paymentFrom(created)!.id);
          const pass = final?.status === 'FAILED';
          return {
            pass,
            httpStatus: created.status,
            message: pass ? 'FAILED as expected' : `Got ${final?.status ?? 'timeout'}`,
            detail: final ? JSON.stringify(final, null, 2) : undefined,
          };
        },
      },
    ],
  },
  {
    id: 'idempotency',
    eyebrow: 'Idempotency',
    title: 'Exactly-once initiation',
    description: 'Verify duplicate Idempotency-Key replays the original response.',
    variant: 'peach',
    tests: [
      {
        id: 'idempotency-create',
        name: 'Create baseline payment',
        description: 'Stores payment id and key for replay tests below.',
        expected: 'HTTP 200, Idempotent-Replayed absent',
        run: async (ctx) => {
          const key = `idempotency-demo-${crypto.randomUUID()}`;
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '42.50',
            currency: 'EUR',
            idempotencyKey: key,
            description: 'Test: idempotency baseline',
          });
          const payment = paymentFrom(created);
          const replayed = created.headers.get('Idempotent-Replayed') === 'true';
          const pass = created.ok && payment != null && !replayed;
          if (pass) {
            ctx.replayIdempotencyKey = key;
            ctx.lastPaymentId = payment.id;
            ctx.lastIdempotencyKey = key;
          }
          return {
            pass,
            httpStatus: created.status,
            message: pass ? `Created ${payment!.id.slice(0, 8)}…` : 'Baseline create failed',
            detail: formatBody(created),
          };
        },
      },
      {
        id: 'idempotency-replay',
        name: 'Replay same Idempotency-Key',
        description: 'Same key and payload — must return the original payment.',
        expected: 'HTTP 200, Idempotent-Replayed: true, same payment id',
        run: async (ctx) => {
          if (!ctx.replayIdempotencyKey) {
            return { pass: false, httpStatus: 0, message: 'Run "Create baseline payment" first' };
          }
          const replay = await createPayment({
            merchantId: ctx.merchantId,
            amount: '42.50',
            currency: 'EUR',
            idempotencyKey: ctx.replayIdempotencyKey,
            description: 'Test: idempotency baseline',
          });
          const payment = paymentFrom(replay);
          const replayed = replay.headers.get('Idempotent-Replayed') === 'true';
          const pass =
            replay.ok &&
            replayed &&
            payment?.id === ctx.lastPaymentId;
          return {
            pass,
            httpStatus: replay.status,
            message: pass ? 'Replay confirmed' : 'Replay mismatch',
            detail: formatBody(replay),
          };
        },
      },
      {
        id: 'idempotency-new-key',
        name: 'New key, same payload',
        description: 'Different Idempotency-Key creates a second payment.',
        expected: 'HTTP 200, new payment id',
        run: async (ctx) => {
          const created = await createPayment({
            merchantId: ctx.merchantId,
            amount: '42.50',
            currency: 'EUR',
            idempotencyKey: crypto.randomUUID(),
            description: 'Test: new idempotency key',
          });
          const payment = paymentFrom(created);
          const pass = created.ok && payment != null && payment.id !== ctx.lastPaymentId;
          return {
            pass,
            httpStatus: created.status,
            message: pass ? `New payment ${payment!.id.slice(0, 8)}…` : 'Expected a different payment id',
            detail: formatBody(created),
          };
        },
      },
    ],
  },
  {
    id: 'validation',
    eyebrow: 'Validation',
    title: 'Client errors (4xx)',
    description: 'Invalid requests rejected before saga execution.',
    variant: 'gray',
    tests: [
      {
        id: 'validation-missing-key',
        name: 'Missing Idempotency-Key',
        description: 'POST without the required header.',
        expected: 'HTTP 400 Bad Request',
        run: async (ctx) => {
          const result = await createPayment({
            merchantId: ctx.merchantId,
            amount: '10.00',
            currency: 'EUR',
            skipIdempotencyHeader: true,
          });
          const pass = result.status === 400;
          return {
            pass,
            httpStatus: result.status,
            message: pass ? 'Rejected as expected' : `Unexpected ${result.status}`,
            detail: formatBody(result),
          };
        },
      },
      {
        id: 'validation-short-key',
        name: 'Idempotency-Key too short',
        description: 'Key "abc" violates the 8–255 character rule.',
        expected: 'HTTP 400 Bad Request',
        run: async (ctx) => {
          const result = await createPayment({
            merchantId: ctx.merchantId,
            amount: '10.00',
            currency: 'EUR',
            idempotencyKey: 'abc',
          });
          const pass = result.status === 400;
          return {
            pass,
            httpStatus: result.status,
            message: pass ? 'Invalid key rejected' : `Unexpected ${result.status}`,
            detail: formatBody(result),
          };
        },
      },
      {
        id: 'validation-zero-amount',
        name: 'Zero amount',
        description: 'Amount must be positive (@DecimalMin 0.0001).',
        expected: 'HTTP 400 Validation Failed',
        run: async (ctx) => {
          const result = await createPayment({
            merchantId: ctx.merchantId,
            amount: '0',
            currency: 'EUR',
            idempotencyKey: crypto.randomUUID(),
          });
          const pass = result.status === 400;
          return {
            pass,
            httpStatus: result.status,
            message: pass ? 'Validation failed as expected' : `Unexpected ${result.status}`,
            detail: formatBody(result),
          };
        },
      },
      {
        id: 'validation-bad-merchant',
        name: 'Invalid merchant UUID',
        description: 'Malformed merchantId in request body.',
        expected: 'HTTP 400 Bad Request',
        run: async (_ctx) => {
          const result = await createPayment({
            merchantId: 'not-a-valid-uuid',
            amount: '10.00',
            currency: 'EUR',
            idempotencyKey: crypto.randomUUID(),
          });
          const pass = result.status === 400;
          return {
            pass,
            httpStatus: result.status,
            message: pass ? 'Invalid merchant rejected' : `Unexpected ${result.status}`,
            detail: formatBody(result),
          };
        },
      },
    ],
  },
  {
    id: 'queries',
    eyebrow: 'Queries',
    title: 'Read operations',
    description: 'GET payment status — polling endpoint used by the UI.',
    variant: 'dark',
    tests: [
      {
        id: 'query-existing',
        name: 'Get existing payment',
        description: 'GET /payments/{id} for the last created payment.',
        expected: 'HTTP 200 with payment body',
        run: async (ctx) => {
          if (!ctx.lastPaymentId) {
            return { pass: false, httpStatus: 0, message: 'Run a success test first to create a payment' };
          }
          const result = await getPayment(ctx.lastPaymentId);
          const pass = result.ok && paymentFrom(result)?.id === ctx.lastPaymentId;
          return {
            pass,
            httpStatus: result.status,
            message: pass ? `Found ${ctx.lastPaymentId.slice(0, 8)}…` : 'GET failed',
            detail: formatBody(result),
          };
        },
      },
      {
        id: 'query-not-found',
        name: 'Payment not found',
        description: 'GET random UUID that does not exist.',
        expected: 'HTTP 404 Not Found',
        run: async (_ctx) => {
          const result = await getPayment('00000000-0000-0000-0000-000000000099');
          const pass = result.status === 404;
          return {
            pass,
            httpStatus: result.status,
            message: pass ? '404 as expected' : `Unexpected ${result.status}`,
            detail: formatBody(result),
          };
        },
      },
    ],
  },
];

export { DEFAULT_MERCHANT_ID };
