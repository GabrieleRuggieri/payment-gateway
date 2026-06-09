#!/bin/sh
# Entrypoint nginx: sostituisce PAYMENT_API_KEY nel template e avvia il server.
set -eu

export PAYMENT_API_KEY="${PAYMENT_API_KEY:-pgw-demo-key-32chars-minimum!!}"
envsubst '${PAYMENT_API_KEY}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
