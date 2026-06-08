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

const TOTAL_TESTS = TEST_SECTIONS.reduce((sum, section) => sum + section.tests.length, 0);

function emptyRunState(): Record<string, TestRunState> {
  return {};
}

/** API test collection — folders, requests, run actions and response panel. */
export function TestCollection({ merchantId, onPaymentResult }: TestCollectionProps) {
  const [runStates, setRunStates] = useState<Record<string, TestRunState>>(emptyRunState);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [runningAll, setRunningAll] = useState(false);
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
    setRunningAll(true);
    setRunStates(emptyRunState());
    contextRef.current = createInitialTestContext(merchantId);
    try {
      for (const section of TEST_SECTIONS) {
        for (const test of section.tests) {
          await runTest(test.id);
        }
      }
    } finally {
      setRunningAll(false);
    }
  }, [merchantId, runTest]);

  const passedCount = Object.values(runStates).filter((s) => s.status === 'passed').length;
  const failedCount = Object.values(runStates).filter((s) => s.status === 'failed').length;

  return (
    <section className="test-collection">
      <div className="test-collection__header">
        <div>
          <span className="test-collection__eyebrow">Collection</span>
          <h2 className="test-collection__title">Payment Gateway API</h2>
          <p className="test-collection__desc">
            {TOTAL_TESTS} requests in {TEST_SECTIONS.length} folders — run individually, by folder, or all at once.
          </p>
          {(passedCount > 0 || failedCount > 0) && (
            <p className="test-collection__summary">
              {passedCount} passed · {failedCount} failed
            </p>
          )}
        </div>
        <button
          type="button"
          className="test-collection__run-all"
          onClick={() => void runAll()}
          disabled={runningAll}
        >
          {runningAll ? 'Running…' : 'Run collection'}
        </button>
      </div>

      <div className="test-sections">
        {TEST_SECTIONS.map((section) => (
          <article key={section.id} className={`bento bento--${section.variant} test-section`}>
            <div className="test-section__header">
              <div>
                <span className="bento__eyebrow">{section.eyebrow}</span>
                <h3 className="bento__title">{section.title}</h3>
                <p className="bento__desc">{section.description}</p>
                <span className="test-section__count">{section.tests.length} requests</span>
              </div>
              <button
                type="button"
                className="btn-outline"
                onClick={() => void runSection(section.id)}
              >
                Run folder
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
                        <div className="test-item__request">
                          <span className={`test-method test-method--${test.method.toLowerCase()}`}>
                            {test.method}
                          </span>
                          <code className="test-item__path">{test.path}</code>
                        </div>
                        <strong className="test-item__name">{test.name}</strong>
                        <p className="test-item__desc">{test.description}</p>
                        <p className="test-item__expected">
                          <span className="test-item__expected-label">Tests</span> {test.expected}
                        </p>
                      </div>
                      <div className="test-item__actions">
                        <StatusBadge status={state.status} httpStatus={state.httpStatus} />
                        <button
                          type="button"
                          className="btn-outline btn-outline--compact"
                          onClick={() => void runTest(test.id)}
                          disabled={state.status === 'running'}
                        >
                          {state.status === 'running' ? 'Sending…' : 'Send'}
                        </button>
                        {state.detail && (
                          <button
                            type="button"
                            className="test-item__toggle"
                            onClick={() => setExpanded((prev) => ({ ...prev, [test.id]: !isOpen }))}
                          >
                            {isOpen ? 'Hide body' : 'Body'}
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
                      <div className="test-item__response">
                        <span className="test-item__response-label">Response</span>
                        <pre className="test-item__detail">{state.detail}</pre>
                      </div>
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
      ? 'Sending'
      : status === 'passed'
        ? `${httpStatus ?? 200} OK`
        : `${httpStatus ?? 'Err'} Fail`;

  return <span className={`test-badge test-badge--${status}`}>{label}</span>;
}
