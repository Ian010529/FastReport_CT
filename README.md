# FastReport_CT

FastReport_CT is a full-stack application for generating, reviewing, exporting, and tracking telecom customer reports.

The app supports two runtime profiles:

- `local`: Next.js + Spring Boot + PostgreSQL + RabbitMQ + SSE + Prometheus/Grafana
- `aws`: Lambda Function URL frontend + API Gateway Spring Lambda + RDS PostgreSQL + SQS + Lambda worker + API Gateway WebSocket

## Live Deployment

Frontend:

[https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws](https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws)

Backend API:

[https://n22jjhz0x5.execute-api.eu-west-2.amazonaws.com/prod](https://n22jjhz0x5.execute-api.eu-west-2.amazonaws.com/prod)

WebSocket API:

`wss://tvclnwkvsf.execute-api.eu-west-2.amazonaws.com/prod`

## What It Does

- Captures customer, manager, service, billing, complaint, and network-quality inputs.
- Validates requests and blocks risky duplicate records.
- Accepts valid requests immediately with `202 Accepted`.
- Processes report generation asynchronously.
- Calls a configurable LLM provider through the `LLMService` abstraction.
- Tracks report status as `pending`, `processing`, `completed`, or `failed`.
- Pushes live status updates through SSE locally and WebSocket on AWS.
- Exports completed reports as TXT, PDF, or CSV.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 14, React 18, TypeScript, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.2.5, Spring Web, Spring AMQP, JdbcTemplate |
| Database | PostgreSQL 16 |
| Local Messaging | RabbitMQ 3 with Management UI |
| AWS Runtime | API Gateway, Lambda, Lambda Function URL, SQS, RDS PostgreSQL, API Gateway WebSocket |
| Monitoring | Spring Actuator, Micrometer, Prometheus, Grafana, CloudWatch |
| PDF Export | iText 8 |
| AI API | OpenAI-compatible and Claude provider implementations |

## Quick Start

Create a root `.env` file:

```env
SPRING_PROFILES_ACTIVE=local
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL=gpt-4o-mini
```

Start the local stack:

```bash
docker compose up --build
```

Local services:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend: [http://localhost:8080](http://localhost:8080)
- RabbitMQ Management UI: [http://localhost:15672](http://localhost:15672)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3001](http://localhost:3001)

Default local credentials:

- PostgreSQL: `fastreport` / `fastreport123`
- RabbitMQ: `fastreport` / `fastreport123`
- Grafana: `admin` / `fastreport123`

## AWS Verification

Verify the deployed AWS backend, frontend, CORS, NAT route, SQS/DLQ health, schema migration, and one end-to-end report:

```bash
scripts/aws/verify_aws_deployment.sh \
  --stack-name fastreport-ct \
  --region eu-west-2 \
  --frontend-url https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws
```

Redeploy the AWS frontend:

```bash
scripts/aws/deploy_frontend_lambda_url.sh --stack-name fastreport-ct --region eu-west-2
```

Run only the backend smoke test:

```bash
scripts/aws/verify_aws_backend.sh https://n22jjhz0x5.execute-api.eu-west-2.amazonaws.com/prod
```

## Documentation

- [Architecture](docs/architecture.md)
- [API Reference](docs/api.md)
- [AWS Deployment](docs/aws-deployment.md)
- [Local Development](docs/local-development.md)
- [Monitoring](docs/monitoring.md)
- [Testing and Seeding](docs/testing-and-seeding.md)

## Repository Layout

```text
.
├── backend/
├── frontend/
├── docs/
├── ops/
├── scripts/
├── docker-compose.yml
├── docker-compose.debug.yml
└── template.yaml
```

## Current Limitations

- No authentication or authorization.
- No persistent audit table for override reasons or retry history.
- Duplicate checks still rely on current schema and `created_at` date logic.
- No Flyway or Liquibase migration management yet.
- No dedicated automated test suite for validation and error branches yet.
