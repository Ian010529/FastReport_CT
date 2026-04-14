#!/usr/bin/env bash
set -euo pipefail

STACK_NAME="${FASTREPORT_STACK_NAME:-fastreport-ct}"
REGION="${AWS_REGION:-eu-west-2}"
REPOSITORY_NAME="${FASTREPORT_FRONTEND_REPOSITORY:-fastreport-frontend}"
FUNCTION_NAME="${FASTREPORT_FRONTEND_FUNCTION:-fastreport-frontend}"
ROLE_NAME="${FASTREPORT_FRONTEND_ROLE:-FastReportFrontendLambdaRole}"

usage() {
  cat >&2 <<USAGE
Usage: $0 [--stack-name fastreport-ct] [--region eu-west-2]

Builds and deploys the Next.js frontend to AWS Lambda Function URL.
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
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
IMAGE_TAG="$(git -C "$ROOT_DIR" rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)"
IMAGE_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPOSITORY_NAME}:${IMAGE_TAG}"

stack_output() {
  local key="$1"
  aws cloudformation describe-stacks \
    --region "$REGION" \
    --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='${key}'].OutputValue | [0]" \
    --output text
}

ensure_role() {
  local assume_policy
  assume_policy="$(mktemp)"
  cat > "$assume_policy" <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Service": "lambda.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }
  ]
}
JSON

  if ! aws iam get-role --role-name "$ROLE_NAME" >/dev/null 2>&1; then
    aws iam create-role \
      --role-name "$ROLE_NAME" \
      --assume-role-policy-document "file://$assume_policy" >/dev/null
    aws iam attach-role-policy \
      --role-name "$ROLE_NAME" \
      --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
    sleep 10
  fi
  rm -f "$assume_policy"

  aws iam get-role --role-name "$ROLE_NAME" --query "Role.Arn" --output text
}

HTTP_API_URL="$(stack_output HttpApiUrl)"
WEBSOCKET_URL="$(stack_output WebSocketUrl)"
if [[ -z "$HTTP_API_URL" || "$HTTP_API_URL" == "None" || -z "$WEBSOCKET_URL" || "$WEBSOCKET_URL" == "None" ]]; then
  echo "Could not read HttpApiUrl/WebSocketUrl from stack $STACK_NAME." >&2
  exit 1
fi

echo "Ensuring ECR repository $REPOSITORY_NAME..."
aws ecr describe-repositories --region "$REGION" --repository-names "$REPOSITORY_NAME" >/dev/null 2>&1 || \
  aws ecr create-repository --region "$REGION" --repository-name "$REPOSITORY_NAME" >/dev/null

echo "Logging in to ECR..."
aws ecr get-login-password --region "$REGION" | \
  docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "Building frontend image $IMAGE_URI..."
docker build \
  --platform linux/arm64 \
  --provenance=false \
  -f "$ROOT_DIR/frontend/Dockerfile.lambda" \
  --build-arg NEXT_PUBLIC_RUNTIME=aws \
  --build-arg NEXT_PUBLIC_API_URL="$HTTP_API_URL" \
  --build-arg NEXT_PUBLIC_WS_URL="$WEBSOCKET_URL" \
  -t "$IMAGE_URI" \
  "$ROOT_DIR/frontend"

echo "Pushing frontend image..."
docker push "$IMAGE_URI"

ROLE_ARN="$(ensure_role)"

if aws lambda get-function --region "$REGION" --function-name "$FUNCTION_NAME" >/dev/null 2>&1; then
  echo "Updating Lambda function $FUNCTION_NAME..."
  aws lambda update-function-code \
    --region "$REGION" \
    --function-name "$FUNCTION_NAME" \
    --image-uri "$IMAGE_URI" >/dev/null
  aws lambda wait function-updated --region "$REGION" --function-name "$FUNCTION_NAME"
else
  echo "Creating Lambda function $FUNCTION_NAME..."
  aws lambda create-function \
    --region "$REGION" \
    --function-name "$FUNCTION_NAME" \
    --package-type Image \
    --code ImageUri="$IMAGE_URI" \
    --role "$ROLE_ARN" \
    --architectures arm64 \
    --memory-size 1024 \
    --timeout 30 >/dev/null
  aws lambda wait function-active --region "$REGION" --function-name "$FUNCTION_NAME"
fi

aws lambda update-function-configuration \
  --region "$REGION" \
  --function-name "$FUNCTION_NAME" \
  --environment "Variables={NEXT_PUBLIC_RUNTIME=aws,NEXT_PUBLIC_API_URL=${HTTP_API_URL},NEXT_PUBLIC_WS_URL=${WEBSOCKET_URL},PORT=3000,HOSTNAME=0.0.0.0,AWS_LWA_PORT=3000}" >/dev/null
aws lambda wait function-updated --region "$REGION" --function-name "$FUNCTION_NAME"

if ! aws lambda get-function-url-config --region "$REGION" --function-name "$FUNCTION_NAME" >/dev/null 2>&1; then
  aws lambda create-function-url-config \
    --region "$REGION" \
    --function-name "$FUNCTION_NAME" \
    --auth-type NONE >/dev/null
fi

aws lambda add-permission \
  --region "$REGION" \
  --function-name "$FUNCTION_NAME" \
  --statement-id FunctionUrlPublicAccess \
  --action lambda:InvokeFunctionUrl \
  --principal "*" \
  --function-url-auth-type NONE >/dev/null 2>&1 || true

aws lambda add-permission \
  --region "$REGION" \
  --function-name "$FUNCTION_NAME" \
  --statement-id FunctionUrlInvokePublicAccess \
  --action lambda:InvokeFunction \
  --principal "*" \
  --invoked-via-function-url >/dev/null 2>&1 || true

FRONTEND_URL="$(aws lambda get-function-url-config \
  --region "$REGION" \
  --function-name "$FUNCTION_NAME" \
  --query "FunctionUrl" \
  --output text)"
FRONTEND_URL="${FRONTEND_URL%/}"

echo
echo "Frontend URL: $FRONTEND_URL"
echo "Backend API URL: $HTTP_API_URL"
echo "WebSocket URL: $WEBSOCKET_URL"
