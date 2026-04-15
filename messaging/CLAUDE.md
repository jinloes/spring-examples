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

# Open http://localhost:8080 in two browser tabs to see messages fan out in real time.
# Or publish via REST:
curl -X POST http://localhost:8080/api/messages \
  -H 'Content-Type: text/plain' -d 'hello world'
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
Redis pub/sub is used as a cross-instance fan-out layer. Messages are published to a Redis
channel via `StringRedisTemplate`. A `RedisMessageListenerContainer` receives them and
`RedisMessageForwarder` pushes them to connected WebSocket clients via `SimpMessagingTemplate`.
STOMP over WebSocket (with SockJS fallback) delivers messages to the browser in real time.

## Tests
```bash
./gradlew :messaging:kafka:test
./gradlew :messaging:pubsub:test      # controller test only, no GCP needed
./gradlew :messaging:redis:test       # starts Redis via Testcontainers
```
