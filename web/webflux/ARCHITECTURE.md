# web/webflux — Architecture

Reference and design details for `web/webflux`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Architecture

- **`AlertController`** — reactive REST controller; accepts and stores `Alert` objects
- **`AlertService`** — in-memory store backed by an `ArrayList` (state is not persisted across restarts)
- **`SecurityConfig`** — configures reactive Spring Security with a custom JWT `AuthenticationWebFilter`
- **`JwtServerAuthenticationConverter`** — extracts the `Bearer` token from the `Authorization` header
- **`JwtAuthenticationManager`** — validates the token using `jwt-common`'s `JwtService` (RSA RS256)
- **`JwtToken`** — `AbstractAuthenticationToken` wrapper holding the raw token and resolved `UserDetails`

The `JwtService` is initialized with a fresh RSA-2048 `KeyPair` generated at startup. For testing,
the same `KeyPair` bean is used to sign tokens in `AlertControllerTest`.

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
