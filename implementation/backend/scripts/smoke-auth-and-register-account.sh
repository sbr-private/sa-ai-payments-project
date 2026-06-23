#!/usr/bin/env bash
# Smoke test: health, demo auth, readiness, POST /accounts (SC-001),
# test helper credit, payment initiation (SC-002), statements.
# Prerequisites: MongoDB on localhost:27017, server up with ENABLE_TEST_HELPERS=true.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/v1}"
DEMO_USER="${DEMO_USER:-benchmark@demo}"
ENABLE_TEST_HELPERS="${ENABLE_TEST_HELPERS:-true}"

pretty() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  else
    cat
  fi
}

section() {
  echo ""
  echo "=== $1 ==="
}

fail() {
  echo "error: $1" >&2
  exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl is required"

section "1. Liveness (no auth)"
curl -sS -f "$BASE_URL/health" | pretty

section "2. Login — payer@demo (no X-Demo-User needed)"
curl -sS -f -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"payer@demo","password":"demo"}' | pretty

section "3. Readiness — requires X-Demo-User + MongoDB"
if curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/ready" | pretty; then
  echo "(database reachable)"
else
  status=$?
  if [[ $status -eq 22 ]]; then
    echo "ready returned non-2xx — is MongoDB running on localhost:27017?" >&2
    exit 1
  fi
  exit $status
fi

section "4. Register account — SC-001 (benchmark@demo)"
ACCOUNT_JSON=$(
  curl -sS -f -X POST "$BASE_URL/accounts" \
    -H "Content-Type: application/json" \
    -H "X-Demo-User: $DEMO_USER" \
    -d '{
      "owner": {
        "nm": "Acme Corp",
        "id": { "othr": { "id": "user_123" } }
      },
      "ccy": "USD"
    }'
)
echo "$ACCOUNT_JSON" | pretty

section "5. Login — support@demo (control centre identity)"
curl -sS -f -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"support@demo","password":"demo"}' | pretty

section "Done"
if command -v jq >/dev/null 2>&1; then
  account_id=$(echo "$ACCOUNT_JSON" | jq -r '.id')
  echo "Registered account id: $account_id"

  section "6. Get account — SC-013 happy path"
  curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/accounts/$account_id" | pretty

  if [[ "$ENABLE_TEST_HELPERS" == "true" ]]; then
    section '7. Credit account — test helper ($1000.00)'
    curl -sS -f -X POST "$BASE_URL/test/accounts/$account_id/credit" \
      -H "Content-Type: application/json" \
      -H "X-Demo-User: $DEMO_USER" \
      -d '{
        "amount": { "value": "1000.00", "ccy": "USD" },
        "endToEndId": "E2E-SMOKE-SEED-0001"
      }'
    echo "(204 No Content)"

    section "8. Verify funded balance"
    curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/accounts/$account_id" | pretty

    section "9. Register creditor account"
    CREDITOR_JSON=$(
      curl -sS -f -X POST "$BASE_URL/accounts" \
        -H "Content-Type: application/json" \
        -H "X-Demo-User: $DEMO_USER" \
        -d '{
          "owner": { "nm": "Supplier Ltd" },
          "ccy": "USD"
        }'
    )
    creditor_id=$(echo "$CREDITOR_JSON" | jq -r '.id')
    echo "$CREDITOR_JSON" | pretty

    section '10. Payment initiation — SC-002 ($50 transfer)'
    curl -sS -f -X POST "$BASE_URL/payment-initiations" \
      -H "Content-Type: application/json" \
      -H "X-Demo-User: $DEMO_USER" \
      -d "{
        \"grpHdr\": {
          \"msgId\": \"MSG-SMOKE-0001\",
          \"creDtTm\": \"2026-06-23T12:00:00Z\",
          \"nbOfTxs\": \"1\",
          \"ctrlSum\": \"50.00\",
          \"initgPty\": { \"nm\": \"Acme Corp\" }
        },
        \"pmtInf\": [{
          \"pmtInfId\": \"PMT-SMOKE-0001\",
          \"pmtMtd\": \"TRF\",
          \"dbtr\": { \"nm\": \"Acme Corp\" },
          \"dbtrAcct\": {
            \"id\": { \"othr\": { \"id\": \"$account_id\" } },
            \"ccy\": \"USD\"
          },
          \"cdtTrfTxInf\": [{
            \"pmtId\": {
              \"instrId\": \"INSTR-SMOKE-0001\",
              \"endToEndId\": \"E2E-SMOKE-0001\"
            },
            \"amt\": { \"instdAmt\": { \"value\": \"50.00\", \"ccy\": \"USD\" } },
            \"cdtr\": { \"nm\": \"Supplier Ltd\" },
            \"cdtrAcct\": {
              \"id\": { \"othr\": { \"id\": \"$creditor_id\" } },
              \"ccy\": \"USD\"
            }
          }]
        }]
      }" | pretty

    section "11. Post-transfer balances"
    echo "Debtor:"
    curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/accounts/$account_id" | pretty
    echo "Creditor:"
    curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/accounts/$creditor_id" | pretty

    section "12. Statements — camt.053"
    echo "Debtor statement:"
    curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/accounts/$account_id/statements" | pretty
    echo "Creditor statement:"
    curl -sS -f -H "X-Demo-User: $DEMO_USER" "$BASE_URL/accounts/$creditor_id/statements" | pretty
  else
    echo "Set ENABLE_TEST_HELPERS=true (and restart server) to run credit/transfer/statement steps."
  fi
else
  echo "Install jq to run the GET /accounts/{id} and SC-002 steps automatically."
fi
