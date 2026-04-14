# Testing and Seeding

Use smoke tests for deployment checks and mock LLM mode for realistic local test traffic.

## Local Build Checks

```bash
cd backend
mvn test
cd ..

scripts/aws/build_lambda_zip.sh

cd frontend
npm run build
cd ..
```

## AWS Smoke Test

Run the complete AWS verification:

```bash
scripts/aws/verify_aws_deployment.sh \
  --stack-name fastreport-ct \
  --region eu-west-2 \
  --frontend-url https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws
```

Run only the backend smoke test:

```bash
scripts/aws/verify_aws_backend.sh https://<http-api-id>.execute-api.eu-west-2.amazonaws.com/prod
```

The backend smoke test:

- calls `/actuator/health`
- creates one report with `POST /api/reports`
- polls `GET /api/reports/{id}`
- exits when the report reaches `completed` or `failed`

## Realistic Seed Data

For realistic test traffic, use the mock LLM provider plus the seeding script instead of inserting rows by hand.

This approach keeps the real chain intact:

- request validation
- inserts into `customers`, `managers`, `reports`, and child tables
- RabbitMQ publishing
- worker processing
- retry behavior from mock LLM failures
- `completed` and `failed` terminal outcomes

A small database backfill step is used only to shape historical timestamps and preserve some `pending` and `processing` records for UI demos.

## Start the Backend in Mock Mode

```bash
LLM_PROVIDER=mock \
LLM_MOCK_FAILURE_RATE=0.08 \
LLM_MOCK_MIN_LATENCY_MS=20 \
LLM_MOCK_MAX_LATENCY_MS=80 \
docker compose up -d --build backend
```

This makes report generation fast, cheap, and predictable for local seeding.

## Seed 100 Reports

```bash
python3 scripts/generate_test_reports.py \
  --count 100 \
  --rate 10 \
  --failed-ratio 0.10 \
  --pending-ratio 0.05 \
  --processing-ratio 0.05 \
  --history-span 3d
```

## Seed 1000 Reports

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

## Seeding Options

- `--rate`: request rate sent to the API
- `--failed-ratio`: final failed share after backfill
- `--pending-ratio`: keep some pending records visible
- `--processing-ratio`: keep some processing records visible
- `--history-span`: spread data across minutes, hours, or days
- `--history-ratio`: how much data should be shifted into older windows
