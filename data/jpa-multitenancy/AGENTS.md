# data/jpa-multitenancy

Demonstrates **schema-per-tenant** multi-tenancy with Spring Data JPA and Hibernate. Each tenant owns a dedicated PostgreSQL schema; Hibernate routes every query to the correct schema based on a tenant identifier extracted from the incoming HTTP request.

## Multi-tenancy pattern

**Schema-per-tenant**: all tenants share the same database and connection pool, but each tenant's data lives in a separate PostgreSQL schema (e.g. `"98b67afd-0d75-4c88-8d81-71821688f345"`). Hibernate's `MultiTenantConnectionProvider` SPI sets `connection.setSchema(tenantId)` before handing the connection to the ORM, so every SQL statement automatically targets the right schema.

## How tenant identity flows through the stack

1. `TenantFilter` — a `OncePerRequestFilter` that reads the `X-Tenant` HTTP header and stores the value in `TenantContext` (a `ThreadLocal`).
2. `TenantContext` — thread-local holder; cleared in the filter's `finally` block to prevent leakage between requests.
3. `TenantIdentifierResolver` — Hibernate SPI (`CurrentTenantIdentifierResolver`) that reads `TenantContext`; falls back to `"public"` when no tenant header is present.
4. `TenantConnectionProvider` — Hibernate SPI (`MultiTenantConnectionProvider`) that calls `connection.setSchema(tenantId)` to point JDBC connections at the tenant's schema.
5. `HibernateConfig` — Spring `@Configuration` that registers both SPI beans, which self-register via `HibernatePropertiesCustomizer`.

The `Order` entity also carries a `@TenantId`-annotated `customerId` field, which Hibernate uses as an additional discriminator filter on queries.

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

## API Endpoints

| Method | Path       | Header              | Body                                      | Description          |
|--------|------------|---------------------|-------------------------------------------|----------------------|
| POST   | `/orders`  | `X-Tenant: <uuid>`  | `{ "name": "<string>", "customerId": "<uuid>" }` | Create an order for a tenant |

### Examples

```bash
TENANT="98b67afd-0d75-4c88-8d81-71821688f345"

# Create an order for tenant
curl -s -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -H "X-Tenant: $TENANT" \
  -d "{\"name\": \"Order 1\", \"customerId\": \"$TENANT\"}"
```

The response includes the generated `id` (UUID). An order created for one tenant is invisible to other tenants — confirmed by the `TenantIsolation` tests.

## Test

Tests use Testcontainers — Docker must be running.

```bash
./gradlew :data:jpa-multitenancy:test
```
