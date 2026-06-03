# messaging/redis

Demonstrates real-time push to browser clients using Redis pub/sub as a cross-instance fan-out
layer and STOMP over WebSocket for the browser connection. Supports two patterns: broadcast to all
connected clients, and user-specific delivery that works correctly regardless of which app instance
handles the REST request.

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

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/messages` | Broadcast a plain-text message to all connected WebSocket clients |
| `POST` | `/api/messages/{username}` | Send a plain-text message to a specific user by username |

Both endpoints return `202 Accepted`.

Examples:

```bash
# Broadcast
curl -X POST http://localhost:8080/api/messages \
  -H 'Content-Type: text/plain' -d 'hello everyone'

# User-specific
curl -X POST http://localhost:8080/api/messages/alice \
  -H 'Content-Type: text/plain' -d 'hello alice'
```

## Demo UI

`src/main/resources/static/index.html` is served at `http://localhost:8080`. It provides:

- A connect panel — enter a username and connect via SockJS/STOMP with `?username=<name>`
- A broadcast panel — messages appear in all connected browser tabs
- A private message panel — enter a recipient username to send a direct message

Open two browser tabs with different usernames to observe both messaging patterns.

## Key Classes

| Class | Role |
|-------|------|
| `MessageController` | REST controller; publishes to Redis `chat` channel for broadcasts and `user-messages` channel (JSON envelope) for user-targeted messages via `StringRedisTemplate` |
| `RedisMessageForwarder` | Redis listener for the `chat` channel; forwards received messages to all WebSocket clients via `SimpMessagingTemplate.convertAndSend("/topic/messages", ...)` |
| `UserMessageForwarder` | Redis listener for the `user-messages` channel; deserializes the `UserMessage` envelope, checks `SimpUserRegistry` for a locally connected session, and delivers via `convertAndSendToUser` if found — silently skips otherwise |
| `UserMessage` | Record acting as the JSON envelope `{to, body}` published on the `user-messages` channel |
| `StompPrincipal` | Minimal `Principal` record that carries the username into the STOMP session; created from the `?username=` query parameter during the WebSocket handshake |
| `RedisConfig` | Declares `RedisMessageListenerContainer` and wires `MessageListenerAdapter` instances for both Redis channels |
| `WebSocketConfig` | Enables STOMP broker (`/topic`, `/queue`), registers the `/ws` SockJS endpoint, and installs a custom `DefaultHandshakeHandler` that reads `?username=` to assign a `StompPrincipal` |

## Architecture

### Broadcast (`POST /api/messages`)

```
REST request (any instance)
  → StringRedisTemplate.convertAndSend("chat", message)
  → Redis pub/sub
  → ALL instances receive
  → RedisMessageForwarder.forwardToWebSocket()
  → SimpMessagingTemplate.convertAndSend("/topic/messages", message)
  → every connected browser receives it
```

### User-specific (`POST /api/messages/{username}`)

```
REST request (any instance)
  → publish JSON envelope {to, body} to Redis "user-messages" channel
  → Redis pub/sub
  → ALL instances receive
  → UserMessageForwarder.forwardToUser(json)
  → each instance calls simpUserRegistry.getUser(username)
      - user NOT connected here → skip (silent no-op)
      - user IS connected here  → convertAndSendToUser(username, "/queue/messages", body)
                                   → only that user's browser receives it
```

`SimpUserRegistry` is instance-local. Publishing through Redis ensures every instance gets the
chance to check and deliver — the correct instance self-selects.

### WebSocket destinations

| Destination | Type | Description |
|-------------|------|-------------|
| `/topic/messages` | Broadcast | All subscribers receive the message |
| `/user/queue/messages` | User-specific | Spring rewrites to a per-session destination; only the target user receives it |

### User identity (demo only)

`WebSocketConfig`'s handshake handler reads a `?username=` query parameter and wraps it in a
`StompPrincipal`. Spring attaches this to the STOMP session so `SimpUserRegistry` and
`convertAndSendToUser` can route by name.

**Production note** — derive the principal from a verified JWT or Spring Security authentication,
never a plain query parameter.

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
