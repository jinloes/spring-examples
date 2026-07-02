# spring-examples

A collection of self-contained Spring Framework example projects covering AI, gRPC, GraphQL, messaging, data, security, and more.

---

## Modules

| Module | Description | Build |
|--------|-------------|-------|
| [`web/http2`](web/http2) | HTTP/2 with Spring Boot | Gradle |
| [`web/loom-web`](web/loom-web) | Project Loom virtual threads — throughput & memory comparison | Gradle |
| [`web/webflux`](web/webflux) | Spring WebFlux / reactive REST | Gradle |
| [`ai/djl`](ai/djl) | Deep Java Library — CodeBERTa language classification | Gradle |
| [`ai/langgraph`](ai/langgraph) | LangChain4j + LangGraph4j customer-support triage agent with LangSmith tracing | Gradle |
| [`ai/todo-mcp`](ai/todo-mcp) | Spring AI MCP server + client | Gradle |
| [`data/elasticsearch`](data/elasticsearch) | Elasticsearch integration | Gradle |
| [`data/jpa-multitenancy`](data/jpa-multitenancy) | JPA multi-tenancy patterns | Gradle |
| [`api-doc`](api-doc) | API docs via Springdoc (annotation-driven) and REST Docs → OpenAPI (test-driven) | Gradle |
| [`datasketches`](datasketches) | Probabilistic analytics — Theta, HLL, Quantiles, Frequency sketches | Gradle |
| [`flowable`](flowable) | Flowable BPM — loan approval workflow with service tasks, user tasks, and gateways | Gradle |
| [`jobrunr`](jobrunr) | JobRunr background jobs — fire-and-forget, delayed, and recurring, with dashboard | Gradle |
| [`docker`](docker) | Docker / containerization examples | Gradle + Maven |
| [`graphql`](graphql) | GraphQL with Spring — queries, mutations, `@BatchMapping` N+1 prevention | Gradle |
| [`grpc/hello-world`](grpc/hello-world) | All four gRPC patterns — unary, server/client/bidirectional streaming | Gradle |
| [`grpc/generic-filters`](grpc/generic-filters) | Protobuf-driven generic field filtering over gRPC | Gradle |
| [`grpc/react-dynamic-components`](grpc/react-dynamic-components) | gRPC + React backend-driven dynamic UI | Gradle |
| [`helm`](helm) | Helm chart examples | Maven |
| [`jwt-common`](jwt-common) | JWT shared library | Gradle |
| [`messaging/kafka`](messaging/kafka) | Apache Kafka producer/consumer | Gradle |
| [`messaging/pubsub`](messaging/pubsub) | GCP Pub/Sub integration | Gradle |
| [`messaging/redis`](messaging/redis) | Redis pub/sub → WebSocket push | Gradle |
| [`nextjs`](nextjs) | Spring Boot + Next.js hello world | Gradle + npm |
| [`oauth2`](oauth2) | OAuth2 examples | Maven |
| [`rate-limiting`](rate-limiting) | Rate limiting examples | Gradle |
| [`react`](react) | React + Spring integration | Maven |
| [`retry`](retry) | Spring Retry | Maven |
| [`salesforce-okta`](salesforce-okta) | Salesforce API integration via Okta JWT token flow | Gradle |
| [`service-discovery`](service-discovery) | Service discovery — Kubernetes DNS, Consul, Spring Cloud Kubernetes | Gradle |

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Gradle | 9.3.1 (wrapper included — use `./gradlew`) |
| Maven | 3.9+ (for Maven-based modules) |
| Docker | Required by modules that use Testcontainers (Kafka, Elasticsearch, etc.) |

---

## Quick Start

### Gradle modules

Each Gradle module is part of the root multi-module build. Run from the repo root:

```bash
# Run a module
./gradlew :<module>:bootRun

# Example
./gradlew :graphql:bootRun
./gradlew :web:http2:bootRun

# Test a module
./gradlew :<module>:test
```

### Maven modules

Maven modules (`docker`, `helm`, `oauth2`, `react`, `retry`) have their own `pom.xml`:

```bash
mvn -f <module>/pom.xml spring-boot:run

# Example
mvn -f retry/pom.xml spring-boot:run
```

---

## Build Systems

This repo uses two build systems depending on the module:

- **Gradle (multi-module)** — the root `settings.gradle` wires together all Gradle modules. Common dependency versions are managed in `build.gradle`. Run everything from the repo root using the Gradle wrapper (`./gradlew`).
- **Maven (standalone)** — each Maven module is self-contained with its own `pom.xml`. Navigate into the module directory or pass `-f <module>/pom.xml` to Maven commands.

---

## Contributing

See [AGENTS.md](AGENTS.md) for coding conventions, commit rules, and agent-specific instructions (Lombok usage, Spotless formatting, module documentation requirements, etc.).

> **Keeping this file up to date:** When you add, remove, or significantly rename a module, update the module table above so it stays in sync with the codebase.
