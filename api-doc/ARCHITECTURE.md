# api-doc — Architecture

Reference and design details for `api-doc`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/accounts/{id}` | Get a bank account by UUID |
| `POST` | `/accounts` | Create a new bank account |

## Architecture Comparison

### openapi sub-module (annotation-driven)

Springdoc scans `@Operation`, `@Parameter`, and `@Schema` annotations at startup and
auto-generates the OpenAPI spec. The spec exists as long as the app is running — no test
required. The tradeoff: annotations can drift from the actual implementation without
any build failure.

### restdoc sub-module (test-driven)

No OpenAPI annotations on the controller. Tests use `MockMvcRestDocumentationWrapper.document()`
with a `resource()` snippet to describe each endpoint. The `openapi3` Gradle task assembles the
snippets into `build/api-spec/openapi.yaml`.

The key guarantee: if a field is added to the response but not described in the test, the test
fails. The spec is enforced to stay in sync with the implementation.
