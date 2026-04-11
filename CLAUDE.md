# spring-examples

A collection of Spring Framework example projects demonstrating various Spring modules and integrations.

## Project Structure

Each top-level directory is a standalone example module:

| Module | Description |
|--------|-------------|
| `cloud-contract/` | Spring Cloud Contract consumer/producer (Maven) |
| `djl/` | Deep Java Library (ML) integration (Gradle) |
| `docker/` | Docker/containerization examples (Gradle + Maven) |
| `elasticsearch/` | Elasticsearch integration (Maven) |
| `eureka/` | Service discovery with Eureka (Maven) |
| `graphql/` | GraphQL with Spring (Maven) |
| `grpc/` | gRPC API with Spring (Gradle) |
| `grpc-react-dynamic-ui/` | gRPC with React dynamic UI (Gradle) |
| `helm/` | Helm chart examples (Maven) |
| `http2/` | HTTP/2 with Spring (Gradle) |
| `jpa-multitenancy/` | JPA multi-tenancy patterns (Gradle) |
| `jwt-common/` | JWT shared library (Gradle) |
| `kafka/` | Kafka integration (Maven) |
| `loom-web/` | Project Loom virtual threads (Gradle) |
| `oauth2/` | OAuth2 examples (Maven) |
| `openapi/` | OpenAPI/Swagger (Gradle) |
| `pubsub/` | Pub/Sub messaging (Maven) |
| `rabbitmq-websockets/` | RabbitMQ + WebSockets (Gradle) |
| `rate-limiting/` | Rate limiting examples (Gradle) |
| `react/` | React + Spring integration (Maven) |
| `restdoc/` | Spring REST Docs (Gradle) |
| `retry/` | Spring Retry (Maven) |
| `springdoc-openapi/` | SpringDoc OpenAPI (Maven) |
| `theta-sketch/` | Theta sketch / probabilistic data structures (Maven) |
| `webflux/` | Spring WebFlux / reactive (Gradle) |
| `salesforce-okta/` | Salesforce API integration via Okta JWT Bearer flow (Gradle) |
| `nextjs/` | Spring Boot + Next.js hello world (Gradle + npm) |

## Build Systems

- **Gradle** (multi-module): `djl`, `http2`, `jpa-multitenancy`, `jwt-common`, `loom-web`, `openapi`, `rate-limiting`, `webflux`, `grpc`, `grpc-react-dynamic-ui`, `rabbitmq-websockets`, `restdoc`, `vertx`
  - Root `settings.gradle` includes these as subprojects
  - Java 17, Gradle 9.3.1
  - Run: `./gradlew :<module>:bootRun` or `./gradlew :<module>:test`

- **Maven**: `cloud-contract`, `docker`, `elasticsearch`, `eureka`, `graphql`, `helm`, `kafka`, `oauth2`, `pubsub`, `react`, `retry`, `springdoc-openapi`, `theta-sketch`
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

## Conventions

- Prefer formatted strings over concatenation — use `String.format(...)` or `formatted(...)` for multi-part strings, and `%s` placeholders in log calls rather than `+` concatenation
- Add comments for non-trivial logic — explain the *why*, not the *what*. Skip comments for self-evident code (getters, simple mappings, obvious conditionals)
- Each module is self-contained and independently runnable
- Use Lombok where applicable — `@Slf4j` for logging, `@RequiredArgsConstructor` for constructor injection, `@Data`/`@Value` for simple POJOs (prefer records for immutable data)
- Tests use JUnit Platform (`useJUnitPlatform()`)
- New Gradle modules should be added to `settings.gradle`
- **After all code changes**, run Spotless to enforce Google Java Format:
  - Single module: `./gradlew :<module>:spotlessApply`
  - All modules: `./gradlew spotlessApply`