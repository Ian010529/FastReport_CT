# Architecture

FastReport_CT has two runtime profiles: a local Docker Compose profile for development and an AWS serverless profile for hosted use.

## Runtime Profiles

### Local

The local runtime uses:

- Next.js frontend
- Spring Boot backend
- PostgreSQL
- RabbitMQ
- Server-Sent Events
- Prometheus and Grafana

Local flow:

1. The user submits a report request from the frontend.
2. The backend validates the payload and runs duplicate-detection checks.
3. If the request is valid, the backend stores the report and returns `202 Accepted`.
4. The backend publishes a RabbitMQ job for asynchronous processing.
5. The frontend opens an SSE connection for the new report.
6. A Spring Boot worker consumes the RabbitMQ job.
7. The worker calls the configured LLM provider to generate the report.
8. The worker updates report status in PostgreSQL to `processing`, `completed`, or `failed`.
9. The worker publishes a result event to a RabbitMQ result queue.
10. The backend listens for the result event and pushes it to connected SSE clients.

### AWS

The AWS runtime uses:

- Lambda Function URL for the Next.js frontend container
- API Gateway HTTP API + Java Lambda for the Spring API
- RDS PostgreSQL for persistence
- SQS for background jobs
- Java Lambda worker for report generation
- API Gateway WebSocket + Java Lambda for live result updates
- NAT Gateway for outbound OpenAI-compatible LLM calls from private Lambda subnets

The AWS frontend connects to API Gateway HTTP API through `NEXT_PUBLIC_API_URL`, and it connects to API Gateway WebSocket through `NEXT_PUBLIC_WS_URL`.

## Async Worker

The async components are:

- `ReportController`: accepts requests and returns success responses
- `ReportApplicationService`: validates input, checks duplicates, persists report data, and publishes jobs
- `ReportJobPublisher`: application port for publishing background jobs
- `RabbitReportJobPublisher`: local RabbitMQ job publisher
- `SqsReportJobPublisher`: AWS SQS job publisher
- `RabbitConfig`: declares local queues, exchanges, and bindings
- `RabbitReportJobListener`: consumes local RabbitMQ report jobs
- `ReportJobLambdaHandler`: consumes AWS SQS report jobs
- `ReportJobProcessor`: shared job orchestration for both local and AWS workers
- `ReportGenerationService`: loads report data, calls the LLM, and updates database status
- `ReportResultNotifier`: application port for terminal status notifications
- `RabbitReportResultNotifier`: publishes local result events
- `RabbitReportResultListener`: forwards local result events to SSE clients
- `ApiGatewayWebSocketReportNotifier`: pushes AWS result events to API Gateway WebSocket subscribers
- `ReportSseService`: manages active emitters by report ID

## Report Status

Reports move through these statuses:

- `pending`
- `processing`
- `completed`
- `failed`

If the LLM call fails, the worker retries up to 3 times.

## Retry Strategy

RabbitMQ retry is implemented with delayed retry queues and dead-letter routing.

Retry schedule:

- Retry 1: 5 seconds
- Retry 2: 10 seconds
- Retry 3: 20 seconds

Each report gets:

- 1 initial processing attempt
- up to 3 retries
- 4 total attempts at most

## Real-Time Updates

Local runtime:

- The frontend opens an SSE connection to `GET /api/reports/{id}/events`.
- `RabbitReportResultListener` consumes result events from RabbitMQ.
- `ReportSseService` manages per-report SSE emitters.
- The frontend updates the UI when a result event is received.

AWS runtime:

- The frontend connects to API Gateway WebSocket using `NEXT_PUBLIC_WS_URL`.
- The frontend subscribes with `{ "action": "subscribeReport", "reportId": 123 }`.
- If WebSocket is unavailable, the frontend falls back to polling `GET /api/reports/{id}`.

## LLM Provider Abstraction

- `ReportGenerationService` builds the report prompt and delegates generation through `LLMServiceFactory`.
- `LLMServiceFactory` selects the active provider from configuration.
- `OpenAICompatibleLLMService` supports OpenAI-style Chat Completions endpoints.
- `ClaudeLLMService` supports Claude-style message endpoints.
- Switching providers is configuration-driven through `llm.provider`.

## Data Model

Main tables:

- `customers`
- `managers`
- `reports`
- `spending_history`
- `complaints`
- `network_quality`

The backend stores all input rows in PostgreSQL and later writes generated report content back into `reports.report_content`.

## Frontend UX

The frontend is structured as a responsive report workspace with reusable UI primitives such as section cards, feedback banners, page headers, and status badges.

The create form distinguishes error types instead of only showing a raw message:

- `VALIDATION_ERROR`: red feedback panel and field highlighting
- `BLOCK`: red feedback panel and conflict field highlighting
- `WARNING`: amber feedback panel and `overrideReason` highlighting
- successful submission: green feedback panel

This keeps frontend control flow stable even if backend wording changes.
