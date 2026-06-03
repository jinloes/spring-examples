# api-doc/openapi

Demonstrates the **annotation-driven** approach to OpenAPI documentation using
[Springdoc OpenAPI](https://springdoc.org/). Springdoc scans `@Operation`,
`@Parameter`, and `@Schema` annotations at startup and auto-generates the spec
— no test run required.

## Approach

- Controllers are annotated with `@Operation`, `@ApiResponse`, and `@Schema` from
  the OpenAPI / Swagger annotations library.
- Springdoc reads those annotations at startup and exposes a live spec at `/v3/api-docs`.
- Swagger UI is served at `/swagger-ui.html`.

The tradeoff: annotations can drift from the actual implementation without any build failure.
Choose this approach when you want zero-friction live docs and the team is disciplined about
keeping annotations in sync.

## How to Run

```bash
./gradlew :api-doc:openapi:bootRun
```

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Interactive Swagger UI |
| http://localhost:8080/v3/api-docs | Raw JSON spec |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/accounts/{id}` | Get a bank account by UUID |
| `POST` | `/accounts` | Create a new bank account (returns 201) |

## How to Test

```bash
./gradlew :api-doc:openapi:test
```

Tests live in `src/test/java/com/jinloes/apidoc/openapi/web/AccountControllerTest.java`
and use `@WebMvcTest` with `MockMvc`. Tests are organised into `@Nested` classes by scenario:

- `WhenGettingAccount` — verifies GET `/accounts/{id}` returns the correct body and ID
- `WhenCreatingAccount` — verifies POST `/accounts` returns 201 with the requested values

## When to Choose This Approach

Choose annotation-driven Springdoc when you want live documentation with minimal setup and your
team is comfortable keeping annotations current. Prefer the `restdoc` approach when you need the
build to fail if the spec goes out of sync with the implementation.
