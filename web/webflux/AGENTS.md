# web/webflux

Demonstrates Spring WebFlux (reactive web) with JWT-based authentication. The application is
fully non-blocking: controllers return `Mono`/`Flux` types, and security uses reactive
`ServerHttpSecurity` instead of the traditional servlet-based filter chain.

## Architecture

- **`AlertController`** — reactive REST controller; accepts and stores `Alert` objects
- **`AlertService`** — in-memory store backed by an `ArrayList` (state is not persisted across restarts)
- **`SecurityConfig`** — configures reactive Spring Security with a custom JWT `AuthenticationWebFilter`
- **`JwtServerAuthenticationConverter`** — extracts the `Bearer` token from the `Authorization` header
- **`JwtAuthenticationManager`** — validates the token using `jwt-common`'s `JwtService` (RSA RS256)
- **`JwtToken`** — `AbstractAuthenticationToken` wrapper holding the raw token and resolved `UserDetails`

The `JwtService` is initialized with a fresh RSA-2048 `KeyPair` generated at startup. For testing,
the same `KeyPair` bean is used to sign tokens in `AlertControllerTest`.

## How to Run

```bash
./gradlew :web:webflux:bootRun
```

Server starts on **port 8080**. The `/actuator/health` endpoint is public; all other endpoints
require a valid JWT.

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/alerts` | Required | Create an alert; returns `201 Created` with the stored alert (ID assigned) |
| `GET` | `/actuator/health` | None | Health check |

### Alert model

```json
{
  "id": "auto-assigned UUID",
  "message": "string",
  "severity": "HIGH | MEDIUM | LOW"
}
```

### Example request

```bash
# First obtain a token (in tests an RSA key is generated at startup — no external IdP)
# For manual testing, generate an RS256 JWT signed with the app's public key.

curl -X POST http://localhost:8080/alerts \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message": "Disk usage high", "severity": "HIGH"}'
```

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
