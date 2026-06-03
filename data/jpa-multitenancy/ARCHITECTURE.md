# data/jpa-multitenancy — Architecture

Reference and design details for `data/jpa-multitenancy`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Multi-tenancy pattern

**Schema-per-tenant**: all tenants share the same database and connection pool, but each tenant's data lives in a separate PostgreSQL schema (e.g. `"98b67afd-0d75-4c88-8d81-71821688f345"`). Hibernate's `MultiTenantConnectionProvider` SPI sets `connection.setSchema(tenantId)` before handing the connection to the ORM, so every SQL statement automatically targets the right schema.

## How tenant identity flows through the stack

1. `TenantFilter` — a `OncePerRequestFilter` that reads the `X-Tenant` HTTP header and stores the value in `TenantContext` (a `ThreadLocal`).
2. `TenantContext` — thread-local holder; cleared in the filter's `finally` block to prevent leakage between requests.
3. `TenantIdentifierResolver` — Hibernate SPI (`CurrentTenantIdentifierResolver`) that reads `TenantContext`; falls back to `"public"` when no tenant header is present.
4. `TenantConnectionProvider` — Hibernate SPI (`MultiTenantConnectionProvider`) that calls `connection.setSchema(tenantId)` to point JDBC connections at the tenant's schema.
5. `HibernateConfig` — Spring `@Configuration` that registers both SPI beans, which self-register via `HibernatePropertiesCustomizer`.

The `Order` entity also carries a `@TenantId`-annotated `customerId` field, which Hibernate uses as an additional discriminator filter on queries.

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
