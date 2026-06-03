# salesforce-okta

Salesforce API integration via the OAuth 2.0 JWT Bearer Flow (RFC 7523). The service exchanges an Okta SSO token for a Salesforce user-context access token and exposes CRUD endpoints for Salesforce sObjects.

## Architecture

- **`SalesforceTokenService`** — Extracts the email from an Okta JWT (without verification), appends an optional username suffix, mints a short-lived RS256 JWT assertion, and POSTs it to the Salesforce token endpoint.
- **`SalesforceClient`** — Per-request `RestClient` rooted at the `instance_url` from the token response. Supports SOQL queries, sObject CRUD.
- **`SalesforceController`** — REST layer; reads `Authorization: Bearer <okta-token>` header, exchanges it, then delegates to `SalesforceClient`.
- **`SalesforceProperties`** (record) — Bound from `salesforce.*` config namespace.
- **`SalesforceTokenResponse`** (record) — Immutable value object for the Salesforce token endpoint response.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/salesforce/query?soql=...` | Execute SOQL query |
| GET | `/salesforce/sobjects/{type}/{id}` | Get a single sObject record |
| POST | `/salesforce/sobjects/{type}` | Create a new sObject record |
| PATCH | `/salesforce/sobjects/{type}/{id}` | Update an sObject record |
| DELETE | `/salesforce/sobjects/{type}/{id}` | Delete an sObject record |

All endpoints require `Authorization: Bearer <okta-token>`.

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
