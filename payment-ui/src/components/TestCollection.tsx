import { useCallback, useEffect, useRef, useState } from 'react';
import {
  TEST_SECTIONS,
  TestContext,
  TestRunState,
  TestRunStatus,
  createInitialTestContext,
} from '../testCollection';

interface TestCollectionProps {
  merchantId: string;
  onPaymentResult?: (paymentId: string) => void;
}

function emptyRunState(): Record<string, TestRunState> {
  return {};
}

/** Postman-style API test sections — success, failure, idempotency, validation, queries. */
export function TestCollection({ merchantId, onPaymentResult }: TestCollectionProps) {
  const [runStates, setRunStates] = useState<Record<string, TestRunState>>(emptyRunState);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const contextRef = useRef<TestContext>(createInitialTestContext(merchantId));

  useEffect(() => {
    contextRef.current.merchantId = merchantId;
  }, [merchantId]);

  const setTestState = useCallback((testId: string, state: TestRunState) => {
    setRunStates((prev) => ({ ...prev, [testId]: state }));
  }, []);

  const runTest = useCallback(
    async (testId: string) => {
      const testCase = TEST_SECTIONS.flatMap((s) => s.tests).find((t) => t.id === testId);
      if (!testCase) return;

      setTestState(testId, { status: 'running' });
      setExpanded((prev) => ({ ...prev, [testId]: true }));

      try {
        const result = await testCase.run(contextRef.current);
        if (result.pass && contextRef.current.lastPaymentId) {
          onPaymentResult?.(contextRef.current.lastPaymentId);
        }
        setTestState(testId, {
          status: result.pass ? 'passed' : 'failed',
          httpStatus: result.httpStatus,
          message: result.message,
          detail: result.detail,
        });
      } catch (e) {
        setTestState(testId, {
          status: 'failed',
          message: e instanceof Error ? e.message : 'Unexpected error',
        });
      }
    },
    [onPaymentResult, setTestState],
  );

  const runSection = useCallback(
    async (sectionId: string) => {
      const section = TEST_SECTIONS.find((s) => s.id === sectionId);
      if (!section) return;
      for (const test of section.tests) {
        await runTest(test.id);
      }
    },
    [runTest],
  );

  const runAll = useCallback(async () => {
    setRunStates(emptyRunState());
    contextRef.current = createInitialTestContext(merchantId);
    for (const section of TEST_SECTIONS) {
      for (const test of section.tests) {
        await runTest(test.id);
      }
    }
  }, [merchantId, runTest]);

  return (
    <section className="test-collection">
      <div className="test-collection__header">
        <div>
          <span className="test-collection__eyebrow">API collection</span>
          <h2 className="test-collection__title">Run the suite</h2>
          <p className="test-collection__desc">
            Postman-style scenarios for success paths, saga failures, idempotency, validation and queries.
          </p>
        </div>
        <button type="button" className="test-collection__run-all" onClick={() => void runAll()}>
          Run all
        </button>
      </div>

      <div className="test-sections">
        {TEST_SECTIONS.map((section) => (
          <article key={section.id} className={`bento bento--${section.variant} test-section`}>
            <div className="test-section__header">
              <div>
                <span className="bento__eyebrow">{section.eyebrow}</span>
                <h3 className="bento__title">{section.title}</h3>
                <p className={`bento__desc${section.variant === 'dark' ? ' bento__desc--light' : ''}`}>
                  {section.description}
                </p>
              </div>
              <button
                type="button"
                className={`btn-outline${section.variant === 'dark' ? ' btn-outline--light' : ''}`}
                onClick={() => void runSection(section.id)}
              >
                Run section
              </button>
            </div>

            <ul className="test-list">
              {section.tests.map((test) => {
                const state = runStates[test.id] ?? { status: 'idle' as TestRunStatus };
                const isOpen = expanded[test.id] ?? false;

                return (
                  <li key={test.id} className="test-item">
                    <div className="test-item__main">
                      <div className="test-item__info">
                        <span className="test-item__method">POST/GET</span>
                        <div>
                          <strong className="test-item__name">{test.name}</strong>
                          <p className="test-item__desc">{test.description}</p>
                          <p className="test-item__expected">Expected: {test.expected}</p>
                        </div>
                      </div>
                      <div className="test-item__actions">
                        <StatusBadge status={state.status} httpStatus={state.httpStatus} />
                        <button
                          type="button"
                          className={`btn-outline${section.variant === 'dark' ? ' btn-outline--light' : ''}`}
                          onClick={() => void runTest(test.id)}
                          disabled={state.status === 'running'}
                        >
                          {state.status === 'running' ? 'Running…' : 'Run'}
                        </button>
                        {state.detail && (
                          <button
                            type="button"
                            className={`test-item__toggle${section.variant === 'dark' ? ' test-item__toggle--light' : ''}`}
                            onClick={() => setExpanded((prev) => ({ ...prev, [test.id]: !isOpen }))}
                          >
                            {isOpen ? 'Hide' : 'Response'}
                          </button>
                        )}
                      </div>
                    </div>

                    {state.message && (
                      <p
                        className={[
                          'test-item__result',
                          state.status === 'passed' ? 'test-item__result--pass' : '',
                          state.status === 'failed' ? 'test-item__result--fail' : '',
                        ]
                          .filter(Boolean)
                          .join(' ')}
                      >
                        {state.message}
                      </p>
                    )}

                    {isOpen && state.detail && (
                      <pre className={`test-item__detail${section.variant === 'dark' ? ' test-item__detail--dark' : ''}`}>
                        {state.detail}
                      </pre>
                    )}
                  </li>
                );
              })}
            </ul>
          </article>
        ))}
      </div>
    </section>
  );
}

function StatusBadge({ status, httpStatus }: { status: TestRunStatus; httpStatus?: number }) {
  if (status === 'idle') return null;

  const label =
    status === 'running'
      ? '…'
      : status === 'passed'
        ? `✓ ${httpStatus ?? ''}`.trim()
        : `✗ ${httpStatus ?? ''}`.trim();

  return <span className={`test-badge test-badge--${status}`}>{label}</span>;
}
