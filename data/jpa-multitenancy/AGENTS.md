# data/jpa-multitenancy

Demonstrates **schema-per-tenant** multi-tenancy with Spring Data JPA and Hibernate. Each tenant owns a dedicated PostgreSQL schema; Hibernate routes every query to the correct schema based on a tenant identifier extracted from the incoming HTTP request.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Requirements

A PostgreSQL instance is required. The app reads standard `spring.datasource.*` properties.

Start PostgreSQL with Docker:

```bash
docker run -d --name pg \
  -p 5432:5432 \
  -e POSTGRES_DB=testdb \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=test \
  postgres:13
```

Configure datasource (e.g. via environment or `application.yaml`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: test
    password: test
  jpa:
    hibernate:
      ddl-auto: create-drop
```

The `schema.sql` file creates the `orders` table in both tenant schemas on startup (`spring.sql.init.mode: always`).

## Run

```bash
./gradlew :data:jpa-multitenancy:bootRun
```

## Test

Tests use Testcontainers — Docker must be running.

```bash
./gradlew :data:jpa-multitenancy:test
```
