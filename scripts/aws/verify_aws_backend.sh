#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 https://<http-api-id>.execute-api.<region>.amazonaws.com/prod" >&2
  exit 2
fi

API_URL="${1%/}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PAYLOAD="$ROOT_DIR/scripts/aws/smoke_create_report.json"
SMOKE_PAYLOAD="$(mktemp)"
trap 'rm -f "$SMOKE_PAYLOAD"' EXIT

UNIQUE_ID="$(date +%s | awk '{print substr($0, length($0) - 7)}')"
NATIONAL_ID="110101${UNIQUE_ID}123X"
sed \
  -e "s/\"customerId\": \"[^\"]*\"/\"customerId\": \"${UNIQUE_ID}\"/" \
  -e "s/\"customerName\": \"[^\"]*\"/\"customerName\": \"AWS Smoke Test ${UNIQUE_ID}\"/" \
  -e "s/\"nationalId\": \"[^\"]*\"/\"nationalId\": \"${NATIONAL_ID}\"/" \
  "$PAYLOAD" > "$SMOKE_PAYLOAD"

echo "Checking health..."
curl -fsS "$API_URL/actuator/health"
echo

echo "Creating report..."
CREATE_RESPONSE="$(curl -fsS -X POST "$API_URL/api/reports" \
  -H 'Content-Type: application/json' \
  -d @"$SMOKE_PAYLOAD")"
echo "$CREATE_RESPONSE"

REPORT_ID="$(printf '%s' "$CREATE_RESPONSE" | sed -n 's/.*"id":\([0-9][0-9]*\).*/\1/p')"
if [[ -z "$REPORT_ID" ]]; then
  echo "Could not parse report id from create response" >&2
  exit 1
fi

echo "Polling report $REPORT_ID..."
for _ in $(seq 1 40); do
  REPORT="$(curl -fsS "$API_URL/api/reports/$REPORT_ID")"
  STATUS="$(printf '%s' "$REPORT" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')"
  echo "status=$STATUS"
  if [[ "$STATUS" == "completed" || "$STATUS" == "failed" ]]; then
    echo "$REPORT"
    exit 0
  fi
  sleep 3
done

echo "Report $REPORT_ID did not reach a terminal status within 120 seconds" >&2
exit 1
