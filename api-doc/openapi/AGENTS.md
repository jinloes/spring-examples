# api-doc/openapi

Demonstrates the **annotation-driven** approach to OpenAPI documentation using
[Springdoc OpenAPI](https://springdoc.org/). Springdoc scans `@Operation`,
`@Parameter`, and `@Schema` annotations at startup and auto-generates the spec
— no test run required.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :api-doc:openapi:bootRun
```

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Interactive Swagger UI |
| http://localhost:8080/v3/api-docs | Raw JSON spec |

## How to Test

```bash
./gradlew :api-doc:openapi:test
```

Tests live in `src/test/java/com/jinloes/apidoc/openapi/web/AccountControllerTest.java`
and use `@WebMvcTest` with `MockMvc`. Tests are organised into `@Nested` classes by scenario:

- `WhenGettingAccount` — verifies GET `/accounts/{id}` returns the correct body and ID
- `WhenCreatingAccount` — verifies POST `/accounts` returns 201 with the requested values
