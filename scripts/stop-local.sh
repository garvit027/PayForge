#!/usr/bin/env bash
# Stops the services started by scripts/start-local.sh. Infrastructure keeps running.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="${LOG_DIR:-$ROOT/target/local-logs}"

for pidfile in "$LOG_DIR"/*.pid; do
  [ -e "$pidfile" ] || continue
  pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    echo "stopped $(basename "$pidfile" .pid) ($pid)"
  fi
  rm -f "$pidfile"
done
