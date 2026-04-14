# Local Development

The local runtime uses Docker Compose to start the frontend, backend, PostgreSQL, RabbitMQ, Prometheus, and Grafana.

## Environment Variables

Create a `.env` file in the project root:

```env
SPRING_PROFILES_ACTIVE=local
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL=gpt-4o-mini
```

## Start the Default Stack

```bash
docker compose up --build
```

Services:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend: [http://localhost:8080](http://localhost:8080)
- PostgreSQL: `localhost:5432`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: [http://localhost:15672](http://localhost:15672)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3001](http://localhost:3001)

Default credentials:

- PostgreSQL database: `fastreport`
- PostgreSQL user: `fastreport`
- PostgreSQL password: `fastreport123`
- RabbitMQ user: `fastreport`
- RabbitMQ password: `fastreport123`
- Grafana user: `admin`
- Grafana password: `fastreport123`

## Debug Mode

Use:

```bash
docker compose -f docker-compose.yml -f docker-compose.debug.yml up --build
```

Debug mode adds:

- backend Java remote debug on port `5005`
- frontend `next dev` mode
- bind-mounted frontend source for easier breakpoint debugging

VSCode helpers are included in:

- `.vscode/launch.json`
- `.vscode/tasks.json`

## Local Frontend Against AWS

Copy `HttpApiUrl` and `WebSocketUrl` from the SAM outputs:

```bash
cd frontend
NEXT_PUBLIC_RUNTIME=aws \
NEXT_PUBLIC_API_URL=https://<http-api-id>.execute-api.eu-west-2.amazonaws.com/prod \
NEXT_PUBLIC_WS_URL=wss://<websocket-id>.execute-api.eu-west-2.amazonaws.com/prod \
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

If Next.js selects another local port, add that origin to the backend `AllowedOrigins` parameter before testing browser requests.
