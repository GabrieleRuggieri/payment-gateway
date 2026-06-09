import { describe, expect, it } from 'vitest';
import { SAGA_STEPS, TERMINAL_STATUSES } from './types';

describe('payment status model', () => {
  it('defines terminal saga outcomes', () => {
    expect(TERMINAL_STATUSES).toEqual(['SETTLED', 'FAILED', 'REFUNDED']);
  });

  it('orders saga milestones from initiation to settlement', () => {
    expect(SAGA_STEPS.map((step) => step.status)).toEqual([
      'INITIATED',
      'AUTHORIZED',
      'CAPTURED',
      'SETTLED',
    ]);
  });
});
