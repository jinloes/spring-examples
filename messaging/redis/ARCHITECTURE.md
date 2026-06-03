# messaging/redis — Architecture

Reference and design details for `messaging/redis`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
