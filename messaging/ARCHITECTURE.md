# messaging — Architecture

Reference and design details for `messaging`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
