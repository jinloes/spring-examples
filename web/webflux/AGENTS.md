# web/webflux

Demonstrates Spring WebFlux (reactive web) with JWT-based authentication. The application is
fully non-blocking: controllers return `Mono`/`Flux` types, and security uses reactive
`ServerHttpSecurity` instead of the traditional servlet-based filter chain.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :web:webflux:bootRun
```

Server starts on **port 8080**. The `/actuator/health` endpoint is public; all other endpoints
require a valid JWT.

## Authentication

All requests (except `/actuator/health`) must include an `Authorization: Bearer <jwt>` header.
The JWT must be RS256-signed. The app generates a new RSA key pair on every startup, so tokens
from previous runs are invalid.

## How to Test

```bash
./gradlew :web:webflux:test
```

`AlertControllerTest` uses `@WebFluxTest` with `WebTestClient`. It signs tokens with the
in-context `KeyPair` bean and verifies:
- `POST /alerts` with a valid token returns `201 Created`
- `POST /alerts` without a token returns `401 Unauthorized`
