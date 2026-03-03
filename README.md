# FastReport_CT(v0.1)

An MVP system for generating customer service optimization reports.

Workflow: frontend form input → backend persistence and LLM call → structured Chinese report generation → list, detail view, and file download (TXT/PDF/CSV).

## 1. Overview

FastReport_CT is a full-stack project focused on generating customer service optimization reports:

- Frontend provides report creation and history search.
- Backend uses Spring Boot + JdbcTemplate to read/write PostgreSQL.
- Reports are generated through an OpenAI-compatible API in Markdown format.
- Export is supported in TXT, PDF, and CSV.

## 2. Tech Stack

| Module | Technology |
|---|---|
| Frontend | Next.js 14, React 18, TypeScript, Tailwind CSS |
| Backend | Java 17, Spring Boot 3.2.5, Spring Web, JdbcTemplate |
| Database | PostgreSQL 16 |
| Document Export | iText 8 (PDF) |
| LLM API | OpenAI-compatible Chat Completions API |
| Deployment | Docker Compose |

## 3. Repository Structure

```text
.
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ct/fastreport/
│       │   ├── Application.java
│       │   ├── CorsConfig.java
│       │   └── ReportController.java
│       └── resources/
│           ├── application.yml
│           └── schema.sql
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/app/
        ├── layout.tsx
        ├── page.tsx
        └── report/[id]/page.tsx
```

## 4. Features

### 4.1 Report Creation

- Page: `/`
- Input fields: customer profile, plan, additional services, last 6-month spending, complaint history, and network quality.
- Synchronous backend flow after submit:
  1. Insert record with `pending`
  2. Update status to `processing`
  3. Call LLM to generate report
  4. Set status to `completed` on success, `failed` on error

### 4.2 Report Query

- Supports report history and keyword search (customer ID/name, manager, service code, etc.).
- Detail page: `/report/{id}`.

### 4.3 Report Download

- TXT: plain text report
- PDF: generated with iText (with CJK font fallback logic)
- CSV: UTF-8 BOM included for better Excel compatibility

## 5. API Summary

| Method | Path | Description |
|---|---|---|
| POST | `/api/reports` | Create and generate report (synchronous) |
| GET | `/api/reports` | List reports (supports `search` query) |
| GET | `/api/reports/{id}` | Get report detail |
| GET | `/api/reports/{id}/download?format=txt\|pdf\|csv` | Download report |

### Example Request: POST /api/reports

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

## 6. Run with Docker

### 6.1 Prepare Environment Variables

Create a `.env` file in the project root with at least:

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL=gpt-4o-mini
```

### 6.2 Start Services

```bash
docker compose up --build
```

### 6.3 Access Endpoints

- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- Database: localhost:5432 (DB: `fastreport`, user: `fastreport`, password: `fastreport123`)

## 7. Key Configuration

- CORS allows `http://localhost:3000`.
- Frontend points to backend via `NEXT_PUBLIC_API_URL` (default in Compose: `http://localhost:8080`).
- Backend datasource can be overridden by:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## 8. Version Notes (v0.1)

- Implemented: core report lifecycle (create, list/search, detail, download).
- MVP limitations:
  - No advanced input validation or authorization model yet.
  - LLM generation is synchronous and latency depends on model response time.
  - Backend logic is centralized in `ReportController`; service/repository layering can be added later.
