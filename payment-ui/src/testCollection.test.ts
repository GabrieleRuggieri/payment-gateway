import { describe, expect, it } from 'vitest';
import { TEST_SECTIONS } from './testCollection';

describe('API test collection', () => {
  it('covers success, failure, idempotency, validation and query folders', () => {
    const sectionIds = TEST_SECTIONS.map((section) => section.id);
    expect(sectionIds).toEqual(['success', 'failures', 'idempotency', 'validation', 'queries']);
  });

  it('documents async saga expectations for failure scenarios', () => {
    const failureTests = TEST_SECTIONS.find((section) => section.id === 'failures')!.tests;
    expect(failureTests.map((test) => test.id)).toEqual(['fail-settlement-limit', 'fail-auth-limit']);
    expect(failureTests[0].expected).toContain('REFUNDED');
    expect(failureTests[1].expected).toContain('FAILED');
  });

  it('documents BFF proxy behaviour in the validation folder', () => {
    const validationTests = TEST_SECTIONS.find((section) => section.id === 'validation')!.tests;
    const bffTest = validationTests.find((test) => test.id === 'bff-no-client-api-key');
    expect(bffTest?.expected).toContain('no API key in bundle');
  });
});
