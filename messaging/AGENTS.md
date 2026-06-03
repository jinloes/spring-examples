# messaging

Three sub-modules demonstrating different messaging backends with Spring Boot.

| Sub-module | Backend | Use case |
|---|---|---|
| `kafka` | Apache Kafka | High-throughput service-to-service event streaming |
| `pubsub` | GCP Pub/Sub | Managed cloud messaging (requires GCP credentials) |
| `redis` | Redis pub/sub + WebSocket | Real-time push to browser clients |

## How to Run

### kafka
```bash
docker-compose -f messaging/kafka/docker-compose.yml up -d
./gradlew :messaging:kafka:bootRun

# Publish a message:
curl -X POST http://localhost:8080/api/topics/messages \
  -H 'Content-Type: text/plain' -d 'hello world'
```

### pubsub
```bash
# Requires GCP credentials
export ENCODED_KEY=<base64-encoded service account JSON>
export PUBSUB_SUBSCRIPTION=<your-subscription-name>
./gradlew :messaging:pubsub:bootRun

# Publish a message:
curl -X POST http://localhost:8080/api/topics/<your-topic> \
  -H 'Content-Type: text/plain' -d 'hello world'
```

### redis
```bash
docker-compose -f messaging/redis/docker-compose.yml up -d
./gradlew :messaging:redis:bootRun

# Open http://localhost:8080 in two browser tabs with different usernames.
# Broadcast to all:
curl -X POST http://localhost:8080/api/messages \
  -H 'Content-Type: text/plain' -d 'hello everyone'

# Send to a specific user:
curl -X POST http://localhost:8080/api/messages/alice \
  -H 'Content-Type: text/plain' -d 'hello alice'
```

## Architecture

### kafka
Producer sends to a Kafka topic via `KafkaTemplate`. A `@KafkaListener` consumer reads from
the same topic. Uses KRaft mode (no ZooKeeper). Tests use `@EmbeddedKafka`.

### pubsub
Publisher uses `PubSubTemplate` to send to a GCP topic. Consumer is wired via
`PubSubInboundChannelAdapter` + `@ServiceActivator` (Spring Integration channel). The controller
test mocks `PubSubTemplate` so no GCP credentials are needed to run tests.

### redis
Demonstrates two messaging patterns — broadcast and user-specific — both using Redis as the
cross-instance fan-out layer so delivery is correct regardless of which app instance handles
the REST request.

#### Broadcast (`POST /api/messages`)
```
REST request (any instance)
  → StringRedisTemplate.convertAndSend("chat", message)
  → Redis pub/sub
  → ALL instances receive
  → RedisMessageForwarder.forwardToWebSocket()
  → SimpMessagingTemplate.convertAndSend("/topic/messages", message)
  → every connected browser receives it
```

#### User-specific (`POST /api/messages/{username}`)
```
REST request (any instance)
  → publish JSON envelope {to, body} to Redis "user-messages" channel
  → Redis pub/sub
  → ALL instances receive
  → UserMessageForwarder.forwardToUser(json)
  → each instance calls simpUserRegistry.getUser(username)
      ├─ user NOT connected here → skip (silent no-op)
      └─ user IS connected here  → convertAndSendToUser(username, "/queue/messages", body)
                                    → only that user's browser receives it
```

`SimpUserRegistry` is instance-local. Publishing through Redis ensures every instance gets
the chance to check and deliver — the correct instance self-selects.

#### User identity
The WebSocket handshake handler in `WebSocketConfig` reads a `?username=` query parameter
and wraps it in a `StompPrincipal`. Spring attaches this to the STOMP session so
`SimpUserRegistry` and `convertAndSendToUser` can route by name.

**Demo only** — production should derive the principal from a verified JWT or Spring
Security authentication, never a plain query parameter.

## Tests
```bash
./gradlew :messaging:kafka:test
./gradlew :messaging:pubsub:test      # controller test only, no GCP needed
./gradlew :messaging:redis:test       # starts Redis via Testcontainers
```
