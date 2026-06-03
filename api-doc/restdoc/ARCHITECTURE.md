# api-doc/restdoc — Architecture

Reference and design details for `api-doc/restdoc`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Approach

- No OpenAPI annotations on the controller — the contract lives entirely in tests.
- Each test calls `document("operation-id", resource(...))` to describe request/response fields.
- Snippets are written to `build/generated-snippets/` during the test run.
- The `openapi3` Gradle task reads the snippets and produces `build/api-spec/openapi.yaml`.
- If a field is added to the response but not described in a test, **the test fails** — the spec
  is always in sync with the implementation.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/accounts/{id}` | Get a bank account by UUID |
| `POST` | `/accounts` | Create a new bank account (returns 201) |

## When to Choose This Approach

Choose test-driven REST Docs when you need a hard guarantee that the spec matches the
implementation — the build breaks if they diverge. It requires more test boilerplate than
annotation-driven Springdoc but produces a spec that is always trustworthy.
