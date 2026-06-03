# messaging/pubsub — Architecture

Reference and design details for `messaging/pubsub`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/topics/{topic}` | Publish a plain-text message to the named GCP Pub/Sub topic |

Example:

```bash
curl -X POST http://localhost:8080/api/topics/my-topic \
  -H 'Content-Type: text/plain' -d 'hello world'
```

Returns `202 Accepted`.

## Key Classes

| Class | Role |
|-------|------|
| `MessageController` | REST controller; uses `PubSubTemplate` to publish to any topic specified in the URL path |
| `MessageConsumer` | Spring `@Configuration` that wires the inbound pipeline: creates a `PubSubInboundChannelAdapter` bound to the configured subscription and a `PublishSubscribeChannel`, then a `@ServiceActivator` that receives messages, logs the payload, and manually acknowledges each one |
| `PubSubApplication` | Spring Boot entry point |

## Architecture

```
REST POST /api/topics/{topic}
  → PubSubTemplate.publish(topic, message)    (outbound)
  → GCP Pub/Sub topic

GCP Pub/Sub subscription
  → PubSubInboundChannelAdapter               (inbound, AckMode.MANUAL)
  → inputMessageChannel (PublishSubscribeChannel)
  → @ServiceActivator MessageConsumer.onMessage()
      → log payload
      → message.ack()
```
