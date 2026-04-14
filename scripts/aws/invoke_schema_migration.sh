#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <schema-migration-function-name>" >&2
  exit 2
fi

FUNCTION_NAME="$1"
REGION="${AWS_REGION:-eu-west-2}"
OUTPUT_FILE="${FASTREPORT_SCHEMA_MIGRATION_OUTPUT:-/tmp/fastreport-schema-migration-response.json}"

aws lambda invoke \
  --region "$REGION" \
  --function-name "$FUNCTION_NAME" \
  --cli-binary-format raw-in-base64-out \
  --payload '{}' \
  "$OUTPUT_FILE"

echo
echo "Schema migration response:"
cat "$OUTPUT_FILE"
echo
