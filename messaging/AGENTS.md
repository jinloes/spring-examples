# messaging

Three sub-modules demonstrating different messaging backends with Spring Boot.

| Sub-module | Backend | Use case |
|---|---|---|
| `kafka` | Apache Kafka | High-throughput service-to-service event streaming |
| `pubsub` | GCP Pub/Sub | Managed cloud messaging (requires GCP credentials) |
| `redis` | Redis pub/sub + WebSocket | Real-time push to browser clients |

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

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

## Tests
```bash
./gradlew :messaging:kafka:test
./gradlew :messaging:pubsub:test      # controller test only, no GCP needed
./gradlew :messaging:redis:test       # starts Redis via Testcontainers
```
