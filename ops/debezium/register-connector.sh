#!/bin/sh
# Registra (o aggiorna) il connector Debezium Outbox verso Kafka Connect.
set -eu

CONNECT_URL="${CONNECT_URL:-http://debezium-connect:8083}"
CONNECTOR_TEMPLATE="${CONNECTOR_TEMPLATE:-/connector/outbox-connector.json}"
POSTGRES_USER="${POSTGRES_USER:-payments_user}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-payments_pass}"
POSTGRES_DB="${POSTGRES_DB:-payments}"

echo "Waiting for Kafka Connect at ${CONNECT_URL}..."
ATTEMPTS=90
i=1
while [ "$i" -le "$ATTEMPTS" ]; do
  if curl -sf "${CONNECT_URL}/connectors" >/dev/null 2>&1; then
    break
  fi
  sleep 2
  i=$((i + 1))
done

if ! curl -sf "${CONNECT_URL}/connectors" >/dev/null 2>&1; then
  echo "Kafka Connect not ready after ${ATTEMPTS} attempts" >&2
  exit 1
fi

TMP="$(mktemp)"
sed \
  -e "s/\"database.user\": \"[^\"]*\"/\"database.user\": \"${POSTGRES_USER}\"/" \
  -e "s/\"database.password\": \"[^\"]*\"/\"database.password\": \"${POSTGRES_PASSWORD}\"/" \
  -e "s/\"database.dbname\": \"[^\"]*\"/\"database.dbname\": \"${POSTGRES_DB}\"/" \
  "$CONNECTOR_TEMPLATE" > "$TMP"

NAME=$(sed -n 's/.*"name"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$TMP" | head -n 1)
echo "Registering connector ${NAME}..."

if curl -sf "${CONNECT_URL}/connectors/${NAME}" >/dev/null 2>&1; then
  echo "Connector exists — recreating..."
  curl -sf -X DELETE "${CONNECT_URL}/connectors/${NAME}" >/dev/null || true
  sleep 2
fi

curl -sf -X POST -H "Content-Type: application/json" --data @"${TMP}" \
  "${CONNECT_URL}/connectors" >/dev/null

rm -f "$TMP"
echo "Debezium outbox connector registered."
sleep 3
curl -sf "${CONNECT_URL}/connectors/${NAME}/status" || true
echo
