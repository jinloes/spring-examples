# jwt-common

Shared JWT utility library used by other modules in this repo (e.g., `web/webflux`). This is a
plain library — it has no `main` class and cannot be run as a standalone application.

## Key Class

### `JwtService` (`com.jinloes.jwt.JwtService`)

Wraps JJWT to provide stateless JWT validation and claims extraction using an RSA public key.

| Method | Description |
|---|---|
| `JwtService(PublicKey)` | Constructs the service; builds a `JwtParser` that verifies RS256 signatures against the provided public key |
| `boolean isTokenValid(String token)` | Returns `true` if the token has a valid signature, is well-formed, and (if an expiry is present) is not expired |
| `Claims getClaims(String token)` | Parses and returns the token payload; throws a JJWT exception if the token is invalid |

`isTokenValid` handles `ExpiredJwtException`, `MalformedJwtException`, and `SignatureException`
by logging at DEBUG level and returning `false`, so callers only need to branch on the boolean.

## Dependencies

- `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` — JWT parsing and validation
- `org.projectlombok:lombok` — `@Slf4j` for logging
- `org.slf4j:slf4j-api` — compile-only; the consuming application provides the implementation

## How to Build

```bash
./gradlew :jwt-common:build
```

This produces `jwt-common/build/libs/jwt-common.jar`. Other Gradle modules depend on it via
`implementation project(':jwt-common')`.

## How to Test

```bash
./gradlew :jwt-common:test
```

Note: there are currently no standalone tests in this module; coverage is provided by the
consuming modules (e.g., `web/webflux`'s `AlertControllerTest`).
