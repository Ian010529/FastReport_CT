# Monitoring

The project includes a first-pass Prometheus and Grafana monitoring stack for the local backend, plus AWS-native checks for the hosted runtime.

## Local Monitoring

Instrumented areas:

- Spring Boot Actuator and Micrometer via `GET /actuator/prometheus`
- business counters for accepted reports
- terminal report status metrics
- scheduled retry metrics
- LLM request success, error, and latency metrics
- SSE publish success and error metrics
- RabbitMQ queue depth gauges for the main queue, retry queues, and result queue
- JVM, HikariCP, and HTTP server metrics

Services:

- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3001](http://localhost:3001)
- default Grafana credentials: `admin` / `fastreport123`
- optional overrides: `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`, `GRAFANA_PORT`

Start the stack:

```bash
docker compose up -d --build
```

Verify local monitoring:

```bash
curl -s http://localhost:8080/actuator/prometheus | head
curl -s http://localhost:9090/-/ready
```

Grafana auto-loads the `FastReport Overview` dashboard from `ops/grafana/dashboards/fastreport-overview.json`.

## RabbitMQ Checks

Open:

[http://localhost:15672](http://localhost:15672)

Login with:

- username: `fastreport`
- password: `fastreport123`

Queues include:

- `report.generate.queue`
- `report.generate.retry.1.queue`
- `report.generate.retry.2.queue`
- `report.generate.retry.3.queue`
- `report.result.queue`

Backend logs:

```bash
docker compose logs -f backend
```

Typical log sequence:

- report created
- job published
- worker picked message
- LLM call started
- report completed or retry scheduled

Database check:

```bash
docker compose exec db psql -U fastreport -d fastreport -c "select id, status, updated_at from reports order by id desc limit 10;"
```

## AWS Checks

For the AWS runtime, use CloudWatch, SQS, and the verification scripts:

```bash
scripts/aws/verify_aws_deployment.sh \
  --stack-name fastreport-ct \
  --region eu-west-2 \
  --frontend-url https://4pwarn5yejaew4stsx6vp6baf40azdzg.lambda-url.eu-west-2.on.aws
```

Success indicators:

- CloudFormation stack is `UPDATE_COMPLETE`.
- API health returns `UP`.
- Worker Lambda has `LLM_PROVIDER=openai-compatible`.
- Worker Lambda has a non-empty `OPENAI_API_KEY`.
- NAT Gateway is `available`.
- Private Lambda subnets have a `0.0.0.0/0 -> NatGateway` route.
- Main SQS queue returns to `0`.
- DLQ remains `0`.
- Worker CloudWatch logs show the OpenAI-compatible provider call.
