#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "Building and starting PayForge. The first build downloads Maven dependencies."
docker compose up --build -d

echo
echo "Waiting for the gateway..."
until curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; do
  sleep 3
done

cat <<'EOF'

PayForge is up.

  Gateway          http://localhost:8080
  Keycloak         http://localhost:8180   (admin / admin)
  Prometheus       http://localhost:9090
  Kafka            localhost:29092

Demo users: alice/alice123, bob/bob123, admin/admin123

Next: ./scripts/smoke-test.sh
EOF
