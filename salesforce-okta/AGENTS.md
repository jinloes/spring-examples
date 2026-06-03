# salesforce-okta

Salesforce API integration via the OAuth 2.0 JWT Bearer Flow (RFC 7523). The service exchanges an Okta SSO token for a Salesforce user-context access token and exposes CRUD endpoints for Salesforce sObjects.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Required Configuration (env vars)

| Variable | Description |
|----------|-------------|
| `SALESFORCE_CLIENT_ID` | Connected app Consumer Key |
| `SALESFORCE_PRIVATE_KEY` | RSA private key in PKCS8 PEM format (include headers; use `\n` for newlines in env vars) |
| `SALESFORCE_TOKEN_URL` | Token endpoint (default: `https://login.salesforce.com/services/oauth2/token`) |
| `SALESFORCE_AUDIENCE` | JWT audience (default: `https://test.salesforce.com`) |
| `SALESFORCE_USERNAME_SUFFIX` | Suffix appended to Okta email, e.g. `.qa` (default: empty) |

## Run

```bash
./gradlew :salesforce-okta:bootRun
```

## Test

```bash
./gradlew :salesforce-okta:test
./gradlew :salesforce-okta:spotlessApply
```
