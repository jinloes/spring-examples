# salesforce-okta — Architecture

Reference and design details for `salesforce-okta`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
