# api-doc/restdoc

Demonstrates the **test-driven** approach to OpenAPI documentation using
[Spring REST Docs](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)
combined with the [restdocs-api-spec](https://github.com/ePages-de/restdocs-api-spec) plugin.
Tests describe each endpoint via `resource()` snippets; the `openapi3` Gradle task assembles
those snippets into a valid OpenAPI 3 spec.

## Approach

- No OpenAPI annotations on the controller — the contract lives entirely in tests.
- Each test calls `document("operation-id", resource(...))` to describe request/response fields.
- Snippets are written to `build/generated-snippets/` during the test run.
- The `openapi3` Gradle task reads the snippets and produces `build/api-spec/openapi.yaml`.
- If a field is added to the response but not described in a test, **the test fails** — the spec
  is always in sync with the implementation.

## How to Generate the Spec

```bash
# Tests must run first so their snippets are available to the openapi3 task
./gradlew :api-doc:restdoc:test :api-doc:restdoc:openapi3
```

Generated spec location: `api-doc/restdoc/build/api-spec/openapi.yaml`

## How to Run the App

```bash
./gradlew :api-doc:restdoc:bootRun
```

The app runs on http://localhost:8080. There is no live Swagger UI — the spec is a static
artifact produced by the build, not served at runtime.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/accounts/{id}` | Get a bank account by UUID |
| `POST` | `/accounts` | Create a new bank account (returns 201) |

## How to Test

```bash
./gradlew :api-doc:restdoc:test
```

Tests live in `src/test/java/com/jinloes/apidoc/restdoc/web/AccountControllerTest.java`
and use `@WebMvcTest` + `@AutoConfigureRestDocs`. Tests are organised into `@Nested` classes
by scenario:

- `WhenGettingAccount` — documents GET `/accounts/{id}` and verifies the response body
- `WhenCreatingAccount` — documents POST `/accounts` and verifies the 201 response

## When to Choose This Approach

Choose test-driven REST Docs when you need a hard guarantee that the spec matches the
implementation — the build breaks if they diverge. It requires more test boilerplate than
annotation-driven Springdoc but produces a spec that is always trustworthy.
