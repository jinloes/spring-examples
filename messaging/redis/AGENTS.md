# messaging/redis

Demonstrates real-time push to browser clients using Redis pub/sub as a cross-instance fan-out
layer and STOMP over WebSocket for the browser connection. Supports two patterns: broadcast to all
connected clients, and user-specific delivery that works correctly regardless of which app instance
handles the REST request.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Prerequisites

A running Redis instance is required. A `docker-compose.yml` is provided that starts Redis 7:

```bash
docker-compose -f messaging/redis/docker-compose.yml up -d
```

Redis listens on `localhost:6379`.

## How to Run

```bash
# Start Redis first (see Prerequisites above), then:
./gradlew :messaging:redis:bootRun
```

The application starts on port 8080. Open `http://localhost:8080` in a browser to use the demo UI.

## Demo UI

`src/main/resources/static/index.html` is served at `http://localhost:8080`. It provides:

- A connect panel — enter a username and connect via SockJS/STOMP with `?username=<name>`
- A broadcast panel — messages appear in all connected browser tabs
- A private message panel — enter a recipient username to send a direct message

Open two browser tabs with different usernames to observe both messaging patterns.

## Configuration (`application.yml`)

| Property | Default | Description |
|----------|---------|-------------|
| `spring.data.redis.host` | `localhost` | Redis server hostname |
| `spring.data.redis.port` | `6379` | Redis server port |

## How to Test

Tests use Testcontainers to spin up a real Redis container — Docker must be running:

```bash
./gradlew :messaging:redis:test
```

`RedisWebSocketApplicationTest` verifies that both REST endpoints return `202 Accepted`. The
user-targeted test publishes to Redis for a user (`alice`) who is not connected — the endpoint
still returns `202` because the publish succeeds; delivery simply does not occur.
