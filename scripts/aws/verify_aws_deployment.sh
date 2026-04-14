#!/usr/bin/env bash
set -euo pipefail

STACK_NAME="${FASTREPORT_STACK_NAME:-fastreport-ct}"
REGION="${AWS_REGION:-eu-west-2}"
RUN_SCHEMA="true"
RUN_SMOKE="true"
FRONTEND_URL="${FASTREPORT_FRONTEND_URL:-}"

usage() {
  cat >&2 <<USAGE
Usage: $0 [--stack-name fastreport-ct] [--region eu-west-2] [--frontend-url https://...] [--skip-schema] [--skip-smoke]

Verifies the FastReport AWS deployment, including production LLM wiring.
The smoke test creates one report by calling POST /api/reports.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --stack-name)
      STACK_NAME="$2"
      shift 2
      ;;
    --region)
      REGION="$2"
      shift 2
      ;;
    --skip-schema)
      RUN_SCHEMA="false"
      shift
      ;;
    --frontend-url)
      FRONTEND_URL="${2%/}"
      shift 2
      ;;
    --skip-smoke)
      RUN_SMOKE="false"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

stack_output() {
  local key="$1"
  aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='${key}'].OutputValue | [0]" \
    --output text
}

resource_id() {
  local logical_id="$1"
  aws cloudformation describe-stack-resource \
    --region "$REGION" \
    --stack-name "$STACK_NAME" \
    --logical-resource-id "$logical_id" \
    --query "StackResourceDetail.PhysicalResourceId" \
    --output text
}

queue_depth() {
  local queue_url="$1"
  aws sqs get-queue-attributes \
    --region "$REGION" \
    --queue-url "$queue_url" \
    --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible ApproximateNumberOfMessagesDelayed \
    --query "Attributes" \
    --output table
}

echo "Verifying stack $STACK_NAME in $REGION..."

HTTP_API_URL="$(stack_output HttpApiUrl)"
WEBSOCKET_URL="$(stack_output WebSocketUrl)"
SCHEMA_FUNCTION="$(stack_output SchemaMigrationFunctionName)"
REPORT_QUEUE_URL="$(stack_output ReportJobsQueueUrl)"
REPORT_DLQ_URL="$(stack_output ReportJobsDlqUrl)"
NAT_GATEWAY_ID="$(stack_output NatGatewayId)"
WORKER_FUNCTION="$(resource_id WorkerFunction)"

for value_name in HTTP_API_URL WEBSOCKET_URL SCHEMA_FUNCTION REPORT_QUEUE_URL REPORT_DLQ_URL NAT_GATEWAY_ID WORKER_FUNCTION; do
  if [[ -z "${!value_name}" || "${!value_name}" == "None" ]]; then
    echo "Missing required stack value: $value_name" >&2
    exit 1
  fi
done

echo "HTTP API: $HTTP_API_URL"
echo "WebSocket API: $WEBSOCKET_URL"
echo "Worker Lambda: $WORKER_FUNCTION"
echo "NAT Gateway: $NAT_GATEWAY_ID"

echo
echo "Checking Worker Lambda production LLM configuration..."
LLM_PROVIDER="$(aws lambda get-function-configuration \
  --region "$REGION" \
  --function-name "$WORKER_FUNCTION" \
  --query "Environment.Variables.LLM_PROVIDER" \
  --output text)"
OPENAI_KEY_PRESENT="$(aws lambda get-function-configuration \
  --region "$REGION" \
  --function-name "$WORKER_FUNCTION" \
  --query "Environment.Variables.OPENAI_API_KEY != ''" \
  --output text)"

if [[ "$LLM_PROVIDER" != "openai-compatible" ]]; then
  echo "Expected LLM_PROVIDER=openai-compatible, got: $LLM_PROVIDER" >&2
  exit 1
fi

if [[ "$OPENAI_KEY_PRESENT" != "True" ]]; then
  echo "OPENAI_API_KEY is empty in Worker Lambda." >&2
  exit 1
fi

echo "LLM_PROVIDER=openai-compatible"
echo "OPENAI_API_KEY is present and not printed."

echo
echo "Checking NAT Gateway and private default route..."
NAT_STATE="$(aws ec2 describe-nat-gateways \
  --region "$REGION" \
  --nat-gateway-ids "$NAT_GATEWAY_ID" \
  --query "NatGateways[0].State" \
  --output text)"
if [[ "$NAT_STATE" != "available" ]]; then
  echo "NAT Gateway is not available: $NAT_STATE" >&2
  exit 1
fi

PRIVATE_ROUTE_TARGETS="$(aws ec2 describe-route-tables \
  --region "$REGION" \
  --filters "Name=route.nat-gateway-id,Values=$NAT_GATEWAY_ID" \
  --query "RouteTables[].Routes[?DestinationCidrBlock=='0.0.0.0/0' && NatGatewayId=='${NAT_GATEWAY_ID}' && State=='active'].NatGatewayId[]" \
  --output text)"
if [[ "$PRIVATE_ROUTE_TARGETS" != *"$NAT_GATEWAY_ID"* ]]; then
  echo "No active private default route found for NAT Gateway $NAT_GATEWAY_ID." >&2
  exit 1
fi

echo "NAT Gateway is available and has an active private default route."

if [[ -n "$FRONTEND_URL" ]]; then
  echo
  echo "Checking frontend URL..."
  curl -fsSI "$FRONTEND_URL" >/dev/null
  echo "Frontend returned HTTP success: $FRONTEND_URL"

  echo
  echo "Checking backend CORS for frontend origin..."
  CORS_HEADER="$(curl -fsSI \
    -H "Origin: $FRONTEND_URL" \
    "$HTTP_API_URL/api/reports/page" | tr -d '\r' | awk -F': ' 'tolower($1)=="access-control-allow-origin"{print $2}')"
  if [[ "$CORS_HEADER" != "$FRONTEND_URL" ]]; then
    echo "Expected access-control-allow-origin=$FRONTEND_URL, got: ${CORS_HEADER:-<missing>}" >&2
    exit 1
  fi
  echo "Backend CORS allows frontend origin."
fi

echo
echo "Queue depth before smoke test:"
echo "Report queue:"
queue_depth "$REPORT_QUEUE_URL"
echo "DLQ:"
queue_depth "$REPORT_DLQ_URL"

if [[ "$RUN_SCHEMA" == "true" ]]; then
  echo
  "$ROOT_DIR/scripts/aws/invoke_schema_migration.sh" "$SCHEMA_FUNCTION"
fi

if [[ "$RUN_SMOKE" == "true" ]]; then
  echo
  "$ROOT_DIR/scripts/aws/verify_aws_backend.sh" "$HTTP_API_URL"
fi

echo
echo "Queue depth after smoke test:"
echo "Report queue:"
queue_depth "$REPORT_QUEUE_URL"
echo "DLQ:"
queue_depth "$REPORT_DLQ_URL"

echo
echo "Local frontend command:"
cat <<COMMAND
cd frontend
NEXT_PUBLIC_RUNTIME=aws \\
NEXT_PUBLIC_API_URL=$HTTP_API_URL \\
NEXT_PUBLIC_WS_URL=$WEBSOCKET_URL \\
npm run dev
COMMAND
