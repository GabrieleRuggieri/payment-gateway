#!/bin/sh
# End-to-end saga smoke test against a running Docker Compose stack.
set -eu

API_BASE="${API_BASE:-http://localhost:8080}"
API_KEY="${PAYMENT_API_KEY:-pgw-demo-key-32chars-minimum!!}"
MERCHANT_ID="${MERCHANT_ID:-550e8400-e29b-41d4-a716-446655440000}"
IDEMPOTENCY_KEY="smoke-e2e-$(date +%s)"

echo "Creating payment via ${API_BASE}..."
CREATE_RESPONSE=$(curl -sf -X POST "${API_BASE}/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: ${API_KEY}" \
  -H "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  -d "{\"merchantId\":\"${MERCHANT_ID}\",\"amount\":\"19.99\",\"currency\":\"EUR\",\"description\":\"CI saga smoke\"}")

PAYMENT_ID=$(printf '%s' "$CREATE_RESPONSE" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)

if [ -z "$PAYMENT_ID" ]; then
  echo "Failed to parse payment id from response: $CREATE_RESPONSE" >&2
  exit 1
fi

echo "Polling payment ${PAYMENT_ID}..."
ATTEMPTS=40
INTERVAL=2
ATTEMPT=1
while [ "$ATTEMPT" -le "$ATTEMPTS" ]; do
  STATUS=$(curl -sf -H "X-Api-Key: ${API_KEY}" "${API_BASE}/api/v1/payments/${PAYMENT_ID}" \
    | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)

  echo "  attempt ${ATTEMPT}/${ATTEMPTS}: status=${STATUS:-unknown}"

  if [ "$STATUS" = "SETTLED" ]; then
    echo "Saga completed successfully."
    exit 0
  fi
  if [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "REFUNDED" ]; then
    echo "Unexpected terminal status for happy-path smoke: ${STATUS}" >&2
    exit 1
  fi

  sleep "$INTERVAL"
  ATTEMPT=$((ATTEMPT + 1))
done

echo "Timed out waiting for SETTLED" >&2
exit 1
