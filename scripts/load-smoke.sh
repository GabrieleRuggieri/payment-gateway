#!/bin/sh
# Smoke leggero di carico: N pagamenti paralleli (xargs) contro stack locale già up.
set -eu

API_BASE="${API_BASE:-http://localhost:8080}"
API_KEY="${PAYMENT_API_KEY:-pgw-demo-key-32chars-minimum!!}"
MERCHANT_ID="${MERCHANT_ID:-550e8400-e29b-41d4-a716-446655440000}"
COUNT="${COUNT:-10}"
PARALLEL="${PARALLEL:-5}"

echo "Load smoke: ${COUNT} payments, parallel=${PARALLEL}, api=${API_BASE}"

seq 1 "$COUNT" | xargs -P "$PARALLEL" -I{} sh -c '
  KEY="load-{}-$(date +%s)-$$"
  CODE=$(curl -s -o /tmp/pgw-load-{}.json -w "%{http_code}" -X POST "'"${API_BASE}"'/api/v1/payments" \
    -H "Content-Type: application/json" \
    -H "X-Api-Key: '"${API_KEY}"'" \
    -H "Idempotency-Key: ${KEY}" \
    -d "{\"merchantId\":\"'"${MERCHANT_ID}"'\",\"amount\":\"12.34\",\"currency\":\"EUR\",\"description\":\"load smoke {}\"}")
  echo "payment {} → HTTP ${CODE}"
  test "$CODE" = "200"
'

echo "All create requests accepted. Poll a moment for saga progress..."
sleep 8
echo "Done. Inspect Kafka UI (:8090) / Grafana (:3001 con overlay prod) se attivo."
