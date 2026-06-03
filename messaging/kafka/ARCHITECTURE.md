# messaging/kafka — Architecture

Reference and design details for `messaging/kafka`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
