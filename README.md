# FastReport_CT

FastReport_CT is a full-stack application for generating, reviewing, and tracking telecom customer reports.

The current stack uses:

- `Next.js` for the frontend
- `Spring Boot` + `JdbcTemplate` for the backend
- `PostgreSQL` for persistence
- `RabbitMQ` for asynchronous job delivery and retry
- `Server-Sent Events (SSE)` for real-time report status updates
- A pluggable LLM provider layer for report generation

## Overview

Current flow:

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

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 14, React 18, TypeScript, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.2.5, Spring Web, Spring AMQP, JdbcTemplate |
| Database | PostgreSQL 16 |
| Message Broker | RabbitMQ 3 with Management UI |
| Real-time Push | Server-Sent Events via Spring `SseEmitter` |
| PDF Export | iText 8 |
| AI API | Pluggable `LLMService` abstraction with OpenAI-compatible and Claude implementations |
| Local Runtime | Docker Compose |
| Debugging | VSCode + Chrome DevTools + Java remote debug |

## Repository Structure

```text
.
├── docker-compose.yml
├── docker-compose.debug.yml
├── .vscode/
│   ├── launch.json
│   └── tasks.json
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ct/fastreport/
│       │   ├── Application.java
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── exception/
│       │   ├── messaging/
│       │   ├── repository/
│       │   └── service/
│       └── resources/
│           ├── application.yml
│           ├── mock_data.sql
│           └── schema.sql
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/
        ├── app/
        ├── entities/
        ├── features/
        └── shared/
```

## Features

### Report Workspace

- Page: `/`
- Responsive English-language workspace for report operations
- Accepts customer, manager, service, spending, complaint, network-quality, and override-reason input
- Stores the request immediately, publishes an async RabbitMQ job, and returns `202 Accepted` with `pending` status

### Validation and Duplicate Detection

The backend validates incoming report requests before inserting data or publishing a RabbitMQ job.

Validation checks include:

- required fields
- type and null checks for array values
- format checks for `customerId`, `managerId`, and `nationalId`
- dictionary checks for `serviceCode`

Duplicate-detection rules include:

- same `customer_id` + different `national_id` -> `BLOCK`
- same `manager_id` + different `manager_name` -> `BLOCK`
- same customer + same service + same day -> `BLOCK`
- same customer + same service + different day -> `WARNING`
- same `national_id` + different `customer_id` -> `WARNING`

If a warning is triggered and `overrideReason` is missing, the backend rejects the request.

### Unified Error Handling

The backend uses a unified exception system built around `BaseAppException`, typed subclasses, and `@RestControllerAdvice` for consistent JSON error responses.

Error response format:

```json
{
  "type": "VALIDATION_ERROR",
  "code": "INVALID_CUSTOMER_ID",
  "message": "customerId must be exactly 8 digits.",
  "detail": {},
  "httpStatus": 400
}
```

The frontend API layer parses this structure into a typed `ApiError`, so UI code can branch on `type` and `code` instead of raw message text.

### Asynchronous Report Generation

- Background processing is handled by a Spring Boot worker
- Worker status transitions: `pending`, `processing`, `completed`, `failed`
- If the LLM call fails, the job is retried up to 3 times
- On completion or final failure, the worker publishes a result event

### LLM Provider Abstraction

- `ReportGenerationService` builds the report prompt and delegates generation through `LLMServiceFactory`
- `LLMServiceFactory` selects the active provider from configuration
- `OpenAICompatibleLLMService` supports OpenAI-style Chat Completions endpoints
- `ClaudeLLMService` shows how to support a second provider without changing report-generation business logic
- Switching providers is configuration-driven through `llm.provider`

### Real-Time Status Updates via SSE

- The frontend opens an SSE connection to `GET /api/reports/{id}/events`
- `ReportResultListener` consumes result events from RabbitMQ
- `ReportSseService` manages per-report SSE emitters
- The frontend updates the UI when a result event is received

### Retry Strategy

RabbitMQ retry is implemented with delayed retry queues and dead-letter routing.

Retry schedule:

- Retry 1: 5 seconds
- Retry 2: 10 seconds
- Retry 3: 20 seconds

Each report gets:

- 1 initial processing attempt
- up to 3 retries
- 4 total attempts at most

### Report Query and Detail View

- `GET /api/reports` lists report history
- `GET /api/reports/{id}` returns one report
- Detail page: `/report/{id}`

### Export

Completed reports can be downloaded as:

- TXT
- PDF
- CSV

## Monitoring

The project includes a first-pass Prometheus + Grafana monitoring stack for the backend.

Instrumented areas:

- Spring Boot Actuator and Micrometer via `GET /actuator/prometheus`
- business counters for accepted reports, terminal report status, scheduled retries, LLM request success/error and latency, and SSE publish success/error
- RabbitMQ queue depth gauges for the main queue, combined retry queues, and result queue
- built-in JVM, HikariCP, and HTTP server metrics

Services:

- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3001](http://localhost:3001)
- default Grafana credentials: `admin` / `fastreport123`
- optional overrides: `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`, `GRAFANA_PORT`

### Start the full stack

```bash
docker compose up -d --build
```

Then verify:

```bash
curl -s http://localhost:8080/actuator/prometheus | head
curl -s http://localhost:9090/-/ready
```

Grafana auto-loads the `FastReport Overview` dashboard from `ops/grafana/dashboards/fastreport-overview.json`.

## Realistic Seed Data

For realistic test traffic, use the mock LLM provider plus the seeding script instead of inserting rows by hand.

### Why this approach

- requests still go through `POST /api/reports`
- validation, inserts, RabbitMQ publishing, worker consumption, retry, and status updates all run
- a small database backfill step is used only to shape historical timestamps and preserve some `pending` / `processing` records for UI demos

### Start the backend in mock mode

```bash
LLM_PROVIDER=mock \
LLM_MOCK_FAILURE_RATE=0.08 \
LLM_MOCK_MIN_LATENCY_MS=20 \
LLM_MOCK_MAX_LATENCY_MS=80 \
docker compose up -d --build backend
```

This makes report generation fast, cheap, and predictable for local seeding.

### Seed 100 reports

```bash
python3 scripts/generate_test_reports.py \
  --count 100 \
  --rate 10 \
  --failed-ratio 0.10 \
  --pending-ratio 0.05 \
  --processing-ratio 0.05 \
  --history-span 3d
```

### Seed 1000 reports

```bash
python3 scripts/generate_test_reports.py \
  --count 1000 \
  --rate 40 \
  --failed-ratio 0.08 \
  --pending-ratio 0.03 \
  --processing-ratio 0.04 \
  --history-span 14d \
  --settle-timeout 180
```

### Useful knobs

- `--rate`: request rate sent to the API
- `--failed-ratio`: final failed share after backfill
- `--pending-ratio`: keep some pending records visible
- `--processing-ratio`: keep some processing records visible
- `--history-span`: spread data across minutes / hours / days
- `--history-ratio`: how much data should be shifted into older windows

### What is real vs. backfilled

- real chain: request validation, inserts into `customers` / `managers` / `reports` / child tables, RabbitMQ publishing, worker processing, retry behavior from mock LLM failures, and `completed` / `failed` terminal outcomes
- database backfill: historical `created_at` / `updated_at` plus preserving some `pending` and `processing` rows for UI and monitoring demos

## Frontend UX

The current frontend is structured as a responsive report workspace with reusable UI primitives such as section cards, feedback banners, page headers, and status badges.

The create form distinguishes error types instead of only showing a raw message:

- `VALIDATION_ERROR` -> red feedback panel + field highlighting
- `BLOCK` -> red feedback panel + conflict field highlighting
- `WARNING` -> amber feedback panel + `overrideReason` highlighting
- successful submission -> green feedback panel

This keeps frontend control flow stable even if backend wording changes.

## API Summary

| Method | Path | Description |
|---|---|---|
| POST | `/api/reports` | Create a report and enqueue async generation |
| GET | `/api/reports` | List reports |
| GET | `/api/reports/{id}` | Get one report |
| GET | `/api/reports/{id}/events` | Open SSE stream for report updates |
| GET | `/api/reports/{id}/download?format=txt\|pdf\|csv` | Download report content |

### Example Create Request

```json
{
  "customerId": "10000001",
  "customerName": "Zhang San",
  "nationalId": "110101199003077758",
  "managerName": "Manager Li",
  "managerId": "200001",
  "serviceCode": "FTTH_500M",
  "currentPlan": "Integrated Plan 199",
  "additionalServices": ["Cloud Disk", "IPTV"],
  "spendingLast6": [199, 199, 210, 185, 199, 220],
  "complaintHistory": ["2024-12 Slow broadband speed", "2025-01 Slow customer service response"],
  "networkQuality": "Download speed occasionally drops below 50% of subscribed bandwidth",
  "overrideReason": "Confirmed by operator after manual review"
}
```

### Example Success Response

```json
{
  "id": 12,
  "status": "pending",
  "message": "Report accepted. It will be generated in the background."
}
```

### Example Warning Response

```json
{
  "type": "WARNING",
  "code": "OVERRIDE_REASON_REQUIRED",
  "message": "Override reason is required for warning cases.",
  "detail": {
    "warnings": ["Same nationalId exists under a different customerId."]
  },
  "httpStatus": 409
}
```

## Data Model

Main tables:

- `customers`
- `managers`
- `reports`
- `spending_history`
- `complaints`
- `network_quality`

The backend stores all input rows in PostgreSQL and later writes the generated care plan back into `reports.report_content`.

## Local Setup

### 1. Prepare Environment Variables

Create a `.env` file in the project root:

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL=gpt-4o-mini
```

### 2. Start the Default Stack

```bash
docker compose up --build
```

Services:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend: [http://localhost:8080](http://localhost:8080)
- PostgreSQL: `localhost:5432`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: [http://localhost:15672](http://localhost:15672)

Default credentials:

- PostgreSQL database: `fastreport`
- PostgreSQL user: `fastreport`
- PostgreSQL password: `fastreport123`
- RabbitMQ user: `fastreport`
- RabbitMQ password: `fastreport123`

## Debug Mode

Use:

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up --build
```

Debug mode adds:

- backend Java remote debug on port `5005`
- frontend `next dev` mode
- bind-mounted frontend source for easier breakpoint debugging

VSCode helpers are already included in:

- `.vscode/launch.json`
- `.vscode/tasks.json`

## How the Async Worker Works

The async components are:

- `ReportController`: accepts requests and returns success responses
- `ReportApplicationService`: validates input, checks duplicates, persists report data, and publishes jobs
- `ReportJobPublisher`: sends initial and retry messages to RabbitMQ
- `RabbitConfig`: declares queues, exchanges, and bindings
- `ReportWorker`: consumes report jobs
- `ReportGenerationService`: loads report data, calls the LLM, and updates database status
- `ReportResultPublisher`: publishes result events
- `ReportResultListener`: forwards result events to SSE clients
- `ReportSseService`: manages active emitters by report ID

## How to Verify RabbitMQ and the Worker

### Option 1: RabbitMQ Management UI

Open:

- [http://localhost:15672](http://localhost:15672)

Login with:

- username: `fastreport`
- password: `fastreport123`

Queues include:

- `report.generate.queue`
- `report.generate.retry.1.queue`
- `report.generate.retry.2.queue`
- `report.generate.retry.3.queue`
- `report.result.queue`

### Option 2: Backend Logs

```bash
docker compose logs -f backend
```

Typical log sequence:

- report created
- job published
- worker picked message
- LLM call started
- report completed or retry scheduled

### Option 3: Database Check

```bash
docker compose exec db psql -U fastreport -d fastreport -c "select id, status, updated_at from reports order by id desc limit 10;"
```

## Current Limitations

- No authentication or authorization
- No persistent audit table for override reasons or retry history
- Duplicate checks still rely on current schema and `created_at` date logic
- No Flyway or Liquibase migration management yet
- No dedicated automated test suite for validation and error branches yet
