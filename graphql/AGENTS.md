# graphql

Spring Boot + Spring for GraphQL bookstore API demonstrating the core GraphQL patterns.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :graphql:bootRun
```

- GraphiQL playground: http://localhost:8080/graphiql
- Schema introspection: printed to stdout on startup

## Tests

Uses `@GraphQlTest` (GraphQL slice test) — loads only the controllers and schema; repositories
are replaced with Mockito mocks. No server or full application context needed.

```bash
./gradlew :graphql:test
```
