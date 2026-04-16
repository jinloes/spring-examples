# spring-examples

A collection of Spring Framework example projects demonstrating various Spring modules and integrations.

## Project Structure

Each top-level directory is a standalone example module:

| Module | Description |
|--------|-------------|
| `web/http2/` | HTTP/2 with Spring (Gradle) |
| `web/loom-web/` | Project Loom virtual threads — throughput + memory comparison (Gradle) |
| `web/webflux/` | Spring WebFlux / reactive (Gradle) |
| `ai/djl/` | Deep Java Library — CodeBERTa language classification (Gradle) |
| `ai/todo-mcp/` | Spring AI MCP server + client example (Gradle) |
| `data/elasticsearch/` | Elasticsearch integration (Gradle) |
| `data/jpa-multitenancy/` | JPA multi-tenancy patterns (Gradle) |
| `api-doc/` | API documentation: Springdoc (annotation-driven) and REST Docs → OpenAPI (test-driven) (Gradle) |
| `datasketches/` | Probabilistic analytics API — Theta, HLL, Quantiles, Frequency sketches (Gradle) |
| `flowable/` | Flowable BPM — loan approval workflow with service tasks, user tasks, and gateways (Gradle) |
| `jobrunr/` | JobRunr background jobs — fire-and-forget, delayed, and recurring jobs with dashboard (Gradle) |
| `docker/` | Docker/containerization examples (Gradle + Maven) |
| `graphql/` | GraphQL with Spring — queries, mutations, @BatchMapping N+1 prevention (Gradle) |
| `grpc/hello-world/` | All four gRPC patterns — unary, server/client/bidirectional streaming (Gradle) |
| `grpc/generic-filters/` | Protobuf-driven generic field filtering over gRPC (Gradle) |
| `grpc/react-dynamic-components/` | gRPC + React backend-driven dynamic UI (Gradle) |
| `helm/` | Helm chart examples (Maven) |
| `jwt-common/` | JWT shared library (Gradle) |
| `messaging/` | Messaging examples: Kafka, GCP Pub/Sub, Redis pub/sub → WebSocket push (Gradle) |
| `nextjs/` | Spring Boot + Next.js hello world (Gradle + npm) |
| `oauth2/` | OAuth2 examples (Maven) |
| `rate-limiting/` | Rate limiting examples (Gradle) |
| `react/` | React + Spring integration (Maven) |
| `retry/` | Spring Retry (Maven) |
| `salesforce-okta/` | Salesforce API integration via Okta JWT Bearer flow (Gradle) |
| `service-discovery/` | Service discovery examples: Kubernetes DNS, Consul, Spring Cloud Kubernetes (Gradle) |

## Build Systems

d- **Gradle** (multi-module): `ai` (submodules: `djl`, `todo-mcp`), `api-doc`, `data` (submodules: `elasticsearch`, `jpa-multitenancy`), `datasketches`, `flowable`, `jobrunr`, `graphql`, `grpc` (submodules: `hello-world`, `generic-filters`, `react-dynamic-components`), `jwt-common`, `messaging`, `nextjs`, `rate-limiting`, `salesforce-okta`, `service-discovery`, `web` (submodules: `http2`, `loom-web`, `webflux`)
  - Root `settings.gradle` includes these as subprojects
  - Java 17, Gradle 9.3.1
  - Run: `./gradlew :<module>:bootRun` or `./gradlew :<module>:test`

- **Maven**: `docker`, `helm`, `oauth2`, `react`, `retry`
  - Each has its own `pom.xml`
  - Run: `mvn -f <module>/pom.xml spring-boot:run`

## Common Dependencies (Gradle root)

- Java 17
- JUnit Jupiter 6.0.3
- Jackson 2.18.2
- JJWT 0.12.6
- Lombok 1.18.38
- Guava 33.3.1
- AssertJ 3.26.3
- Vavr 0.10.3
- Apache Commons Lang3 3.17.0

## Git

- Do NOT auto-commit changes — only commit when explicitly asked
- Do NOT include `Co-Authored-By` lines in commit messages

## Versions

- **Spring Boot**: always use the latest stable release — currently **4.0.5**. Check Maven Central (`https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-web`) before creating or upgrading a module.

## Conventions

- Prefer formatted strings over concatenation — use `String.format(...)` or `formatted(...)` for multi-part strings, and `%s` placeholders in log calls rather than `+` concatenation
- Add comments for non-trivial logic — explain the *why*, not the *what*. Skip comments for self-evident code (getters, simple mappings, obvious conditionals)
- Each module is self-contained and independently runnable
- Use Lombok where applicable — `@Slf4j` for logging, `@RequiredArgsConstructor` for constructor injection, `@Data`/`@Value` for simple POJOs (prefer records for immutable data)
- Tests use JUnit Platform (`useJUnitPlatform()`)
- New Gradle modules should be added to `settings.gradle`
- **After all code changes**, update the CLAUDE.md for any module you modified — keep endpoints, architecture, run instructions, and test commands in sync with the code. If the module has no CLAUDE.md and the change is non-trivial, create one.
- **After all code changes**, run Spotless on only the modified Gradle modules:
  ```bash
  git diff --name-only HEAD | grep '/' | sed 's|/.*||' | sort -u | xargs -I{} ./gradlew :{}:spotlessApply
  ```
  Or for a known module: `./gradlew :<module>:spotlessApply`