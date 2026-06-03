# messaging/pubsub

Demonstrates managed cloud messaging with GCP Pub/Sub and Spring Boot using the Spring Cloud GCP
starter. A REST endpoint publishes string messages to a named GCP topic; an inbound channel adapter
backed by Spring Integration receives messages from a configured subscription and logs them.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Prerequisites

A GCP project with Pub/Sub enabled is required for full end-to-end operation. Two pieces of
configuration must be provided at runtime:

- **`ENCODED_KEY`** — base64-encoded GCP service account JSON with Pub/Sub permissions
- **`PUBSUB_SUBSCRIPTION`** — name of the subscription to consume from (defaults to
  `my-subscription` if unset)

There is no docker-compose for this submodule. For local development without a GCP project, use
the [GCP Pub/Sub emulator](https://cloud.google.com/pubsub/docs/emulator) and point the
Spring Cloud GCP starter at it via `spring.cloud.gcp.pubsub.emulator-host`.

The controller test (`MessageControllerTest`) mocks `PubSubTemplate` and requires no GCP
credentials — see [How to Test](#how-to-test).

## How to Run

```bash
export ENCODED_KEY=<base64-encoded service account JSON>
export PUBSUB_SUBSCRIPTION=<your-subscription-name>
./gradlew :messaging:pubsub:bootRun
```

The application starts on port 8080.

## Configuration (`application.yml`)

| Property / Env var | Default | Description |
|--------------------|---------|-------------|
| `ENCODED_KEY` / `spring.cloud.gcp.credentials.encoded-key` | _(empty)_ | Base64-encoded service account JSON |
| `PUBSUB_SUBSCRIPTION` / `app.pubsub.subscription` | `my-subscription` | Subscription name to consume from |

## How to Test

The `@WebMvcTest` controller test mocks `PubSubTemplate` — no GCP credentials needed:

```bash
./gradlew :messaging:pubsub:test
```

`MessageControllerTest` posts to `/api/topics/my-topic` and asserts a `202 Accepted` response.
