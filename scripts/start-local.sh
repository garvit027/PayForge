#!/usr/bin/env bash
# Runs the five services from the built jars against the docker-compose infrastructure.
# Assumes infra is already up and reachable (see README).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${LOG_DIR:-$ROOT/target/local-logs}"
mkdir -p "$LOG_DIR"

export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-5432}"
export DB_USER="${DB_USER:-payforge}"
export DB_PASSWORD="${DB_PASSWORD:-payforge}"
export KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP:-localhost:29092}"
export JWT_ISSUER="${JWT_ISSUER:-http://localhost:8180/realms/payforge}"
export JWT_JWK_SET_URI="${JWT_JWK_SET_URI:-http://localhost:8180/realms/payforge/protocol/openid-connect/certs}"
export INTERNAL_TOKEN="${INTERNAL_TOKEN:-local-dev-internal-token}"
export REDIS_HOST="${REDIS_HOST:-localhost}"

start() {
  local module="$1" port="$2"
  shift 2
  local jar="$ROOT/$module/target/$module-1.0.0.jar"
  [ -f "$jar" ] || { echo "missing $jar (run: mvn package)"; exit 1; }
  echo "→ $module on :$port"
  env "$@" java -jar "$jar" >"$LOG_DIR/$module.log" 2>&1 &
  echo $! >"$LOG_DIR/$module.pid"
}

start account-service 8081
start ledger-service 8082
start transfer-service 8083 \
  ACCOUNT_URL=http://localhost:8081 \
  LEDGER_URL=http://localhost:8082
start notifications-service 8084
start gateway 8080 \
  ACCOUNT_URL=http://localhost:8081 \
  LEDGER_URL=http://localhost:8082 \
  TRANSFER_URL=http://localhost:8083 \
  NOTIFICATIONS_URL=http://localhost:8084

echo
echo "Logs: $LOG_DIR"
echo "Stop: scripts/stop-local.sh"
