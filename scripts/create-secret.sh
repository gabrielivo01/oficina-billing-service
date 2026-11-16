#!/usr/bin/env bash
set -euo pipefail

: "${JWT_SECRET:?JWT_SECRET is required}"
: "${MONGO_ROOT_PASSWORD:?MONGO_ROOT_PASSWORD is required}"
: "${MONGO_REPLICA_KEY:?MONGO_REPLICA_KEY is required; generate once and preserve it across upgrades}"
MONGO_ROOT_USERNAME="${MONGO_ROOT_USERNAME:-billing_root}"
APP_PAGAMENTO_GATEWAY="${APP_PAGAMENTO_GATEWAY:-fake}"

if [[ ${#JWT_SECRET} -lt 32 ]]; then
    echo "JWT_SECRET must contain at least 32 bytes for HS256." >&2
    exit 1
fi

if [[ "$APP_PAGAMENTO_GATEWAY" == "mercadopago" ]]; then
    : "${MP_ACCESS_TOKEN:?MP_ACCESS_TOKEN is required for Mercado Pago}"
    : "${MP_WEBHOOK_SECRET:?MP_WEBHOOK_SECRET is required for Mercado Pago}"
    : "${MP_NOTIFICATION_URL:?MP_NOTIFICATION_URL is required for Mercado Pago}"
    : "${MP_BACK_URL:?MP_BACK_URL is required for Mercado Pago}"
fi

kubectl create namespace oficina --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic oficina-billing-mongodb \
    --namespace oficina \
    --from-literal=MONGO_ROOT_USERNAME="$MONGO_ROOT_USERNAME" \
    --from-literal=MONGO_ROOT_PASSWORD="$MONGO_ROOT_PASSWORD" \
    --from-literal=MONGO_REPLICA_KEY="$MONGO_REPLICA_KEY" \
    --dry-run=client \
    -o yaml | kubectl apply -f -
kubectl create secret generic oficina-billing-service \
    --namespace oficina \
    --from-literal=APP_PAGAMENTO_GATEWAY="$APP_PAGAMENTO_GATEWAY" \
    --from-literal=JWT_SECRET="$JWT_SECRET" \
    --from-literal=MP_ACCESS_TOKEN="${MP_ACCESS_TOKEN:-}" \
    --from-literal=MP_WEBHOOK_SECRET="${MP_WEBHOOK_SECRET:-}" \
    --from-literal=MP_NOTIFICATION_URL="${MP_NOTIFICATION_URL:-}" \
    --from-literal=MP_BACK_URL="${MP_BACK_URL:-http://localhost:3000/pagamento/retorno}" \
    --dry-run=client \
    -o yaml | kubectl apply -f -
