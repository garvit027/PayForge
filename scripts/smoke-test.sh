#!/usr/bin/env bash
# End-to-end smoke test: registers two customers, funds one, moves money, and proves
# that replaying the same Idempotency-Key does not double-charge.
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
KEYCLOAK="${KEYCLOAK:-http://localhost:8180}"
REALM="${REALM:-payforge}"
INTERNAL_TOKEN="${INTERNAL_TOKEN:-local-dev-internal-token}"
LEDGER="http://localhost:8082"

pass() { printf '  \033[32mok\033[0m   %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; exit 1; }

json() { python3 -c "import json,sys; print(json.load(sys.stdin, parse_float=str, parse_int=str)$1)"; }

echo "→ waiting for the gateway"
for _ in $(seq 1 60); do
  if curl -fsS "$GATEWAY/actuator/health" >/dev/null 2>&1; then break; fi
  sleep 2
done
curl -fsS "$GATEWAY/actuator/health" >/dev/null || fail "gateway never became healthy"

echo "→ fetching an access token for alice"
TOKEN=$(curl -fsS -X POST \
  "$KEYCLOAK/realms/$REALM/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d "grant_type=password&client_id=payforge-api&username=alice&password=alice123" \
  | json '["access_token"]')
[ -n "$TOKEN" ] && pass "token issued"

auth=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

echo "→ registering customers"
ALICE=$(curl -fsS -X POST "$GATEWAY/api/v1/customers" "${auth[@]}" \
  -d '{"fullName":"Alice Fernandes","email":"alice@example.com","country":"IN"}')
BOB=$(curl -fsS -X POST "$GATEWAY/api/v1/customers" "${auth[@]}" \
  -d '{"fullName":"Bob Iyer","email":"bob@example.com","country":"IN"}')
ALICE_ID=$(echo "$ALICE" | json '["id"]')
BOB_ID=$(echo "$BOB" | json '["id"]')
pass "customers registered"

echo "→ opening accounts"
ALICE_ACCOUNT=$(curl -fsS -X POST "$GATEWAY/api/v1/accounts" "${auth[@]}" \
  -d "{\"customerId\":\"$ALICE_ID\",\"currency\":\"INR\"}")
BOB_ACCOUNT=$(curl -fsS -X POST "$GATEWAY/api/v1/accounts" "${auth[@]}" \
  -d "{\"customerId\":\"$BOB_ID\",\"currency\":\"INR\"}")
ALICE_NUMBER=$(echo "$ALICE_ACCOUNT" | json '["accountNumber"]')
BOB_NUMBER=$(echo "$BOB_ACCOUNT" | json '["accountNumber"]')
ALICE_LEDGER=$(echo "$ALICE_ACCOUNT" | json '["ledgerCode"]')
BOB_LEDGER=$(echo "$BOB_ACCOUNT" | json '["ledgerCode"]')
pass "accounts opened: $ALICE_NUMBER / $BOB_NUMBER"

echo "→ funding alice with 5,000 INR"
curl -fsS -X POST "$LEDGER/internal/v1/settlements" \
  -H "X-PayForge-Internal-Token: $INTERNAL_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"accountCode\":\"$ALICE_LEDGER\",\"amount\":5000.00,\"currency\":\"INR\",\"reference\":\"seed-alice\"}" >/dev/null
pass "funded"

echo "→ moving 1,250 INR alice -> bob"
KEY="smoke-$(date +%s)"
FIRST=$(curl -fsS -X POST "$GATEWAY/api/v1/transfers" "${auth[@]}" \
  -H "Idempotency-Key: $KEY" \
  -d "{\"sourceAccountNumber\":\"$ALICE_NUMBER\",\"targetAccountNumber\":\"$BOB_NUMBER\",\"amount\":1250.00,\"currency\":\"INR\",\"note\":\"smoke test\"}")
STATUS=$(echo "$FIRST" | json '["status"]')
[ "$STATUS" = "COMPLETED" ] && pass "transfer $STATUS" || fail "transfer ended as $STATUS"

echo "→ replaying the same Idempotency-Key"
SECOND=$(curl -fsS -X POST "$GATEWAY/api/v1/transfers" "${auth[@]}" \
  -H "Idempotency-Key: $KEY" \
  -d "{\"sourceAccountNumber\":\"$ALICE_NUMBER\",\"targetAccountNumber\":\"$BOB_NUMBER\",\"amount\":1250.00,\"currency\":\"INR\",\"note\":\"smoke test\"}")
[ "$(echo "$FIRST" | json '["id"]')" = "$(echo "$SECOND" | json '["id"]')" ] \
  && pass "same transfer returned, no double charge" \
  || fail "idempotency broken"

echo "→ checking balances"
ALICE_BALANCE=$(curl -fsS "$GATEWAY/api/v1/ledger/accounts/$ALICE_LEDGER/balance" "${auth[@]}" | json '["balance"]')
BOB_BALANCE=$(curl -fsS "$GATEWAY/api/v1/ledger/accounts/$BOB_LEDGER/balance" "${auth[@]}" | json '["balance"]')

echo
echo "  alice: $ALICE_BALANCE INR (expected 3750.0000)"
echo "  bob:   $BOB_BALANCE INR (expected 1250.0000)"
[ "$ALICE_BALANCE" = "3750.0000" ] || fail "alice balance wrong"
[ "$BOB_BALANCE" = "1250.0000" ] || fail "bob balance wrong"

echo "→ waiting for the transfer event to be projected into notifications"
for _ in $(seq 1 30); do
  COUNT=$(curl -fsS "$GATEWAY/api/v1/notifications?accountNumber=$ALICE_NUMBER" "${auth[@]}" \
    | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))' 2>/dev/null || echo 0)
  [ "$COUNT" -gt 0 ] && break
  sleep 1
done

NOTIFICATION=$(curl -fsS "$GATEWAY/api/v1/notifications?accountNumber=$ALICE_NUMBER" "${auth[@]}")
KIND=$(echo "$NOTIFICATION" | python3 -c 'import json,sys; rows=json.load(sys.stdin); print(rows[0]["kind"] if rows else "")')
[ "$KIND" = "TRANSFER_COMPLETED" ] \
  && pass "notification projected: $KIND" \
  || fail "notification feed did not contain TRANSFER_COMPLETED (got '${KIND:-nothing}')"

echo
printf '\033[32mAll checks passed.\033[0m\n'
