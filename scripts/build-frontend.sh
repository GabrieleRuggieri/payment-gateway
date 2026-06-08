#!/bin/sh
# Build Docker image for the payment-ui frontend (also invoked by compose service build-frontend).
set -eu

ROOT="$(CDPATH= cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -z "${COMPOSE_PROJECT_NAME:-}" ]; then
  COMPOSE_PROJECT_NAME=payment-gateway
fi
export COMPOSE_PROJECT_NAME

echo "Building payment-ui image..."
docker compose -f "$ROOT/docker-compose.yml" build payment-ui
echo "Frontend image ready."
