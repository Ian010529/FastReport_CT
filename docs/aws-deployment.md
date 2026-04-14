# AWS Deployment

The AWS deployment uses no EC2 instances and does not replace the local RabbitMQ runtime.

## Runtime

The AWS runtime uses:

- Lambda Function URL for the Next.js frontend
- API Gateway HTTP API + Java Lambda for the Spring API
- SQS + Java Lambda worker for background report generation
- private RDS PostgreSQL for persistence
- API Gateway WebSocket + Java Lambda for live result updates
- NAT Gateway for outbound OpenAI-compatible LLM calls

Default region: `eu-west-2`.

Current hosted URLs:

- Frontend: [https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws](https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws)
- HTTP API: [https://n22jjhz0x5.execute-api.eu-west-2.amazonaws.com/prod](https://n22jjhz0x5.execute-api.eu-west-2.amazonaws.com/prod)
- WebSocket API: `wss://tvclnwkvsf.execute-api.eu-west-2.amazonaws.com/prod`

## Install and Login

Install AWS CLI v2 and AWS SAM CLI, then log in:

```bash
aws configure sso
aws sso login --profile fastreport

export AWS_PROFILE=fastreport
export AWS_REGION=eu-west-2

aws sts get-caller-identity
sam --version
```

## Deploy the Backend Stack

The SAM template creates the VPC, private RDS PostgreSQL, SQS + DLQ, HTTP API Lambda, worker Lambda, WebSocket API, WebSocket Lambda, schema migration Lambda, and a single NAT Gateway for outbound OpenAI-compatible LLM calls from private Lambda subnets.

Load local environment variables before deployment:

```bash
set -a
source .env
set +a
```

Guided deployment:

```bash
sam build
sam deploy --guided --region eu-west-2
```

Recommended guided values:

- Stack Name: `fastreport-ct`
- Region: `eu-west-2`
- Parameter `EnvironmentName`: `fastreport`
- Parameter `DbName`: `fastreport`
- Parameter `DbInstanceClass`: `db.t4g.micro`
- Parameter `AllowedOrigins`: `http://localhost:3000,http://localhost:3001,http://localhost:3002`
- Parameter `LlmProvider`: `openai-compatible`
- Parameter `OpenAiApiKey`: use `$OPENAI_API_KEY` from your local `.env`
- Parameter `OpenAiBaseUrl`: `https://api.openai.com`
- Parameter `OpenAiModel`: `gpt-4o-mini`
- Confirm IAM role creation: yes

The local `.env` file is not automatically uploaded by CloudFormation. Pass `OpenAiApiKey` during deployment so Lambda receives it as `OPENAI_API_KEY`. Do not commit real API keys to the repository.

## Non-Guided Backend Updates

For non-guided updates, keep the existing database password and pass the OpenAI key from `.env`:

```bash
set -a
source .env
set +a

DB_PASSWORD="$(
  aws lambda get-function-configuration \
    --region eu-west-2 \
    --function-name fastreport-ct-ApiFunction-CbDXdrKdr67A \
    --query 'Environment.Variables.SPRING_DATASOURCE_PASSWORD' \
    --output text
)"

sam build
sam deploy \
  --stack-name fastreport-ct \
  --region eu-west-2 \
  --capabilities CAPABILITY_IAM \
  --resolve-s3 \
  --no-confirm-changeset \
  --no-fail-on-empty-changeset \
  --parameter-overrides \
    EnvironmentName=fastreport \
    DbName=fastreport \
    DbUsername=fastreport \
    DbPassword="$DB_PASSWORD" \
    DbInstanceClass=db.t4g.micro \
    AllowedOrigins=http://localhost:3000,http://localhost:3001,http://localhost:3002 \
    LlmProvider=openai-compatible \
    OpenAiApiKey="$OPENAI_API_KEY" \
    OpenAiBaseUrl="${OPENAI_BASE_URL:-https://api.openai.com}" \
    OpenAiModel="${OPENAI_MODEL:-gpt-4o-mini}"
```

## Initialize Schema

After `sam deploy`, copy the `SchemaMigrationFunctionName` output and run:

```bash
scripts/aws/invoke_schema_migration.sh <SchemaMigrationFunctionName>
```

A successful response includes:

```json
{
  "status": "ok",
  "statementsExecuted": 9
}
```

## Verify AWS Backend

Run the full deployment verification:

```bash
scripts/aws/verify_aws_deployment.sh --stack-name fastreport-ct --region eu-west-2
```

Run only the backend smoke test:

```bash
scripts/aws/verify_aws_backend.sh https://<http-api-id>.execute-api.eu-west-2.amazonaws.com/prod
```

Manual health check:

```bash
curl -i https://<http-api-id>.execute-api.eu-west-2.amazonaws.com/prod/actuator/health
```

Manual create:

```bash
curl -i -X POST https://<http-api-id>.execute-api.eu-west-2.amazonaws.com/prod/api/reports \
  -H 'Content-Type: application/json' \
  -d @scripts/aws/smoke_create_report.json
```

Success criteria:

- API returns `202` with `pending`.
- SQS visible messages briefly rises then returns to `0`.
- Worker Lambda CloudWatch logs show `Calling openai-compatible provider at https://api.openai.com/v1/chat/completions`.
- `GET /api/reports/{id}` moves to `completed`.
- DLQ stays at `0`.
- WebSocket subscriptions receive completed or failed updates when configured.
- If WebSocket is unavailable, the frontend polling fallback still reaches terminal status.

## Deploy Frontend to AWS

The frontend is deployed as a Next.js standalone container on AWS Lambda Function URL. This gives the app a public HTTPS URL without any server to start manually.

```bash
scripts/aws/deploy_frontend_lambda_url.sh --stack-name fastreport-ct --region eu-west-2
```

The script prints the final frontend URL:

```text
Frontend URL: https://<function-url-id>.lambda-url.eu-west-2.on.aws
```

## CORS Setup

After the frontend URL exists, redeploy the backend with that URL added to `AllowedOrigins`:

```bash
set -a
source .env
set +a

DB_PASSWORD="$(
  aws lambda get-function-configuration \
    --region eu-west-2 \
    --function-name fastreport-ct-ApiFunction-CbDXdrKdr67A \
    --query 'Environment.Variables.SPRING_DATASOURCE_PASSWORD' \
    --output text
)"

sam build
sam deploy \
  --stack-name fastreport-ct \
  --region eu-west-2 \
  --capabilities CAPABILITY_IAM \
  --resolve-s3 \
  --no-confirm-changeset \
  --no-fail-on-empty-changeset \
  --parameter-overrides \
    EnvironmentName=fastreport \
    DbName=fastreport \
    DbUsername=fastreport \
    DbPassword="$DB_PASSWORD" \
    DbInstanceClass=db.t4g.micro \
    AllowedOrigins=http://localhost:3000,http://localhost:3001,http://localhost:3002,https://<function-url-id>.lambda-url.eu-west-2.on.aws \
    LlmProvider=openai-compatible \
    OpenAiApiKey="$OPENAI_API_KEY" \
    OpenAiBaseUrl="${OPENAI_BASE_URL:-https://api.openai.com}" \
    OpenAiModel="${OPENAI_MODEL:-gpt-4o-mini}"
```

Verify the complete hosted app:

```bash
scripts/aws/verify_aws_deployment.sh \
  --stack-name fastreport-ct \
  --region eu-west-2 \
  --frontend-url https://<function-url-id>.lambda-url.eu-west-2.on.aws
```

Users access the deployed project through the frontend URL. The AWS backend services run automatically.

## Real LLM Providers

The default AWS deployment is OpenAI-compatible production mode. Private Lambda subnets use the NAT Gateway for outbound calls to the configured model provider.

To switch providers, redeploy with `LlmProvider=openai-compatible` or `LlmProvider=claude` and the matching API key parameters.

A NAT Gateway and Elastic IP have hourly and data processing costs.
