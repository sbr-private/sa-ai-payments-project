#!/usr/bin/env bash
# Smoke test: health, demo auth, readiness, POST /accounts (SC-001).
# Prerequisites: MongoDB on localhost:27017, server up (mvn spring-boot:run).
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/v1}"
DEMO_USER="${DEMO_USER:-benchmark@demo}"

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
  echo "GET /accounts/{id} is not implemented yet — save this id for later steps."
else
  echo "Install jq to extract the account id from step 4."
fi
