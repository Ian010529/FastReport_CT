# FastReport_CT

FastReport_CT is a full-stack application for generating telecom customer care plans.

The current implementation uses:

- `Next.js` for the frontend
- `Spring Boot` + `JdbcTemplate` for the backend
- `PostgreSQL` for persistence
- `RabbitMQ` for asynchronous job delivery and retry
- `Server-Sent Events (SSE)` for real-time report status updates to the frontend
- An OpenAI-compatible Chat Completions API for care plan generation

## Branches

| Branch | Description |
|---|---|
| `master` | Stable, merged releases |
| `v0.1` | Initial MVP: RabbitMQ async processing, retry strategy, export, manual refresh only |
| `v0.2` | Current development: SSE real-time push, result listener, structured improvements |

## Overview

Current flow:

1. A user submits a report request from the frontend.
2. The backend stores the request and returns immediately with status `pending`.
3. The backend publishes a RabbitMQ job.
4. The frontend opens an SSE connection to receive real-time status updates.
5. A Spring Boot worker consumes the job.
6. The worker calls the LLM to generate the care plan.
7. The worker updates the report status in PostgreSQL to `processing`, `completed`, or `failed`.
8. The `ReportResultPublisher` publishes the result back to a RabbitMQ result queue.
9. The `ReportResultListener` consumes the result and pushes the update via SSE to any connected frontend clients.
10. The frontend receives the push and updates the UI without a manual refresh.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 14, React 18, TypeScript, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.2.5, Spring Web, Spring AMQP, JdbcTemplate |
| Database | PostgreSQL 16 |
| Message Broker | RabbitMQ 3 with Management UI |
| Real-time Push | Server-Sent Events (SSE) via Spring SseEmitter |
| PDF Export | iText 8 |
| AI API | OpenAI-compatible Chat Completions API |
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
│       │   ├── CorsConfig.java
│       │   ├── RabbitConfig.java
│       │   ├── ReportController.java
│       │   ├── ReportGenerationService.java
│       │   ├── ReportJobMessage.java
│       │   ├── ReportJobPublisher.java
│       │   ├── ReportRequest.java
│       │   ├── ReportResponse.java
│       │   ├── ReportResultListener.java
│       │   ├── ReportResultMessage.java
│       │   ├── ReportResultPublisher.java
│       │   ├── ReportSseService.java
│       │   └── ReportWorker.java
│       └── resources/
│           ├── application.yml
│           ├── mock_data.sql
│           └── schema.sql
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/app/
        ├── globals.css
        ├── layout.tsx
        ├── page.tsx
        └── report/[id]/page.tsx
```

## Features

### Report Creation

- Page: `/`
- Accepts customer, manager, service, spending, complaint, and network-quality input
- Stores the request immediately
- Publishes an async RabbitMQ job
- Returns `202 Accepted` with `pending` status

### Asynchronous Care Plan Generation

- Background processing is handled by a Spring Boot worker
- Worker status transitions:
  - `pending`
  - `processing`
  - `completed`
  - `failed`
- If the LLM call fails, the job is retried up to 3 times
- On completion or failure, the result is published to a dedicated RabbitMQ result queue

### Real-Time Status Updates via SSE

- The frontend opens an SSE connection to `GET /api/reports/{id}/sse` after submitting a request
- `ReportResultListener` consumes messages from the result queue
- `ReportSseService` manages per-report SSE emitters and delivers push events
- The frontend React component updates the UI immediately without polling or manual refresh

### Retry Strategy

RabbitMQ retry is implemented with delayed retry queues and dead-letter routing.

Retry schedule:

- Retry 1: 5 seconds
- Retry 2: 10 seconds
- Retry 3: 20 seconds

This means each report gets:

- 1 initial processing attempt
- up to 3 retries
- 4 total attempts at most

### Report Query and Detail View

- `GET /api/reports` lists report history
- `GET /api/reports/{id}` returns a single report
- Detail page: `/report/{id}`

### Export

Completed reports can be downloaded as:

- TXT
- PDF
- CSV

## UX Behavior

As of v0.1, real-time updates are delivered via SSE:

- After report submission, the detail page opens an SSE connection
- Status transitions (`pending` → `processing` → `completed` / `failed`) are pushed to the browser in real time
- No polling or manual refresh is required on the detail page

The list page (`/`) still reflects the last known state at page load. A full refresh is needed to see updated statuses in the report list.

## API Summary

| Method | Path | Description |
|---|---|---|
| POST | `/api/reports` | Create a report and enqueue async generation |
| GET | `/api/reports` | List reports |
| GET | `/api/reports/{id}` | Get one report |
| GET | `/api/reports/{id}/sse` | Open SSE stream for real-time status updates |
| GET | `/api/reports/{id}/download?format=txt\|pdf\|csv` | Download report content |

### Example Request

`POST /api/reports`

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
  "networkQuality": "Download speed occasionally drops below 50% of subscribed bandwidth"
}
```

### Example Response

```json
{
  "id": 12,
  "status": "pending",
  "message": "Request accepted. The Care Plan will be generated in the background."
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

The backend stores report metadata and all supporting input rows in PostgreSQL, and later writes the generated care plan back into `reports.report_content`.

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

The project keeps production-style and debug-oriented startup modes separate.

### Default Mode

Use only:

```bash
docker compose up --build
```

This uses the regular images and does not expose the Java debug port.

### Debug Mode

Use:

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up --build
```

Debug mode adds:

- backend Java remote debug on port `5005`
- frontend `next dev` mode
- bind-mounted frontend source for easier breakpoint debugging

VSCode debug helpers are already included in:

- `.vscode/launch.json`
- `.vscode/tasks.json`

## How the Async Worker Works

The async components are:

- `ReportController`: creates the report, publishes the first job, and exposes the SSE endpoint
- `ReportJobPublisher`: sends initial and retry messages to the job queue
- `RabbitConfig`: declares the main queue, retry queues, and the result queue
- `ReportWorker`: consumes jobs from RabbitMQ
- `ReportGenerationService`: loads report data, calls the LLM, and updates database status
- `ReportResultPublisher`: publishes result events to the result queue after job completion
- `ReportResultListener`: consumes result events and forwards them to `ReportSseService`
- `ReportSseService`: manages active SSE emitters by report ID and pushes events to connected clients

### Status Lifecycle

The backend updates report status as follows:

- `pending`: the request has been stored and queued
- `processing`: a worker has started handling the message
- `completed`: the care plan was generated and saved
- `failed`: all attempts were exhausted

## How to Verify RabbitMQ and the Worker

### Option 1: RabbitMQ Management UI

Open:

- [http://localhost:15672](http://localhost:15672)

Login with:

- username: `fastreport`
- password: `fastreport123`

You should see queues similar to:

- `report.generate.queue`
- `report.generate.retry.1.queue`
- `report.generate.retry.2.queue`
- `report.generate.retry.3.queue`

What to expect:

- a new report publishes a message to the main queue
- if processing succeeds quickly, the message disappears almost immediately
- if processing fails, the message moves through retry queues before returning to the main queue

### Option 2: Backend Logs

Watch backend logs:

```bash
docker compose logs -f backend
```

Typical log lines:

- report created
- RabbitMQ job published
- worker picked message
- LLM call started
- worker completed report
- or retry scheduled after failure

### Option 3: Database Check

Check report status directly:

```bash
docker compose exec db psql -U fastreport -d fastreport -c "select id, status, updated_at from reports order by id desc limit 10;"
```

### Option 4: Manual UI Refresh

1. Create a report from the frontend
2. Wait for background processing
3. Do not refresh
4. Observe that the page still shows stale data
5. Refresh manually
6. Observe the updated status or completed care plan

This is expected behavior in the current MVP.

## How to Test Retry Behavior

The easiest way to test retry behavior is to intentionally break the LLM configuration.

For example:

1. set an invalid `OPENAI_API_KEY`
2. restart the stack
3. submit a new report
4. watch backend logs and RabbitMQ queues

Expected result:

- the worker fails
- retries are scheduled with delay
- after the final attempt, the report status becomes `failed`

## Current Limitations

- No authentication or authorization
- No input validation beyond basic frontend form behavior
- Backend persistence logic is still fairly controller-centric
- No audit trail for retry history or error details in the database
- No worker scaling controls or concurrency tuning yet
- The report list page does not receive SSE updates; only the detail page does

## Next Improvement Ideas (v0.2 Targets)

- Store failure reason and retry metadata in the database
- Split persistence and query logic into repository/service layers
- Add Flyway or Liquibase for versioned schema migration
- Introduce structured job tables for better observability
- Add tests for worker retry and status transitions
- Extend SSE to the report list page for live status updates across all reports
