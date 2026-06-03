# api-doc/openapi — Architecture

Reference and design details for `api-doc/openapi`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Approach

- Controllers are annotated with `@Operation`, `@ApiResponse`, and `@Schema` from
  the OpenAPI / Swagger annotations library.
- Springdoc reads those annotations at startup and exposes a live spec at `/v3/api-docs`.
- Swagger UI is served at `/swagger-ui.html`.

The tradeoff: annotations can drift from the actual implementation without any build failure.
Choose this approach when you want zero-friction live docs and the team is disciplined about
keeping annotations in sync.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/accounts/{id}` | Get a bank account by UUID |
| `POST` | `/accounts` | Create a new bank account (returns 201) |

## When to Choose This Approach

Choose annotation-driven Springdoc when you want live documentation with minimal setup and your
team is comfortable keeping annotations current. Prefer the `restdoc` approach when you need the
build to fail if the spec goes out of sync with the implementation.
