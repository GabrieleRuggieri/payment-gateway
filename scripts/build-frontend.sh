#!/bin/sh
# Build dell'immagine Docker del frontend payment-ui (invocato anche dal servizio compose build-frontend).
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
