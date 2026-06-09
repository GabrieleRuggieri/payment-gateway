#!/bin/sh
# Build delle immagini Docker di tutti i microservizi backend (invocato anche dal servizio compose build-backend).
set -eu

ROOT="$(CDPATH= cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [ -z "${COMPOSE_PROJECT_NAME:-}" ]; then
  COMPOSE_PROJECT_NAME=payment-gateway
fi
export COMPOSE_PROJECT_NAME

echo "Building backend images: payment-service authorization-service capture-service settlement-service notification-service webhook-receiver"
docker compose -f "$ROOT/docker-compose.yml" build --parallel \
  payment-service \
  authorization-service \
  capture-service \
  settlement-service \
  notification-service \
  webhook-receiver
echo "Backend images ready."
