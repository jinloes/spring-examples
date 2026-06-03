# api-doc/restdoc

Demonstrates the **test-driven** approach to OpenAPI documentation using
[Spring REST Docs](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)
combined with the [restdocs-api-spec](https://github.com/ePages-de/restdocs-api-spec) plugin.
Tests describe each endpoint via `resource()` snippets; the `openapi3` Gradle task assembles
those snippets into a valid OpenAPI 3 spec.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

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

## How to Test

```bash
./gradlew :api-doc:restdoc:test
```

Tests live in `src/test/java/com/jinloes/apidoc/restdoc/web/AccountControllerTest.java`
and use `@WebMvcTest` + `@AutoConfigureRestDocs`. Tests are organised into `@Nested` classes
by scenario:

- `WhenGettingAccount` — documents GET `/accounts/{id}` and verifies the response body
- `WhenCreatingAccount` — documents POST `/accounts` and verifies the 201 response
