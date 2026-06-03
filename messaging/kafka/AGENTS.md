# messaging/kafka

Demonstrates high-throughput service-to-service event streaming with Apache Kafka and Spring Boot.
A REST endpoint publishes string messages to a named Kafka topic; a `@KafkaListener` consumer reads
from the same topic and logs each received message.

## Prerequisites

A running Kafka broker is required. A `docker-compose.yml` is provided that starts Kafka 3.9.0 in
KRaft mode (no ZooKeeper):

```bash
docker-compose -f messaging/kafka/docker-compose.yml up -d
```

The broker listens on `localhost:9092`.

## How to Run

```bash
# Start Kafka first (see Prerequisites above), then:
./gradlew :messaging:kafka:bootRun
```

The application starts on port 8080.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/topics/{topic}` | Publish a plain-text message to the named Kafka topic |

Example:

```bash
curl -X POST http://localhost:8080/api/topics/messages \
  -H 'Content-Type: text/plain' -d 'hello world'
```

Returns `202 Accepted`.

## Key Classes

| Class | Role |
|-------|------|
| `MessageController` | REST controller; uses `KafkaTemplate<String, String>` to publish to any topic specified in the URL path |
| `MessageListener` | `@KafkaListener` that subscribes to the `messages` topic using the configured consumer group; logs each received payload |
| `KafkaApplication` | Spring Boot entry point |

## Configuration (`application.yml`)

| Property | Default | Description |
|----------|---------|-------------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker address |
| `spring.kafka.consumer.group-id` | `messaging-group` | Consumer group for `MessageListener` |
| `spring.kafka.consumer.auto-offset-reset` | `earliest` | Start consuming from the earliest offset when no committed offset exists |

## How to Test

Tests use `@EmbeddedKafka` — no external broker needed:

```bash
./gradlew :messaging:kafka:test
```

`MessageControllerTest` starts an embedded single-partition Kafka broker, posts a message to
`/api/topics/messages`, and asserts a `202 Accepted` response.
