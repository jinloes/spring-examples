# nextjs — Architecture

Reference and design details for `nextjs`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Architecture

- **Backend** (`src/main/java/`) — Spring Boot REST API on port 8080
- **Frontend** (`frontend/`) — Next.js 16 + React 19 app on port 3000 (dev)
  - `app/page.tsx` — server component that fetches `GET /api/hello` at render time
  - `next.config.ts` — rewrites `/api/:path*` to `http://localhost:8080/api/:path*`

## API Endpoint

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hello` | Returns `{"message": "Hello from Spring Boot!"}` |

```bash
curl http://localhost:8080/api/hello
```

## How the Proxy Works

`next.config.ts` defines a rewrite rule so that the browser can call `/api/hello` and Next.js
forwards the request to `http://localhost:8080/api/hello`. This means the backend URL is never
exposed to the browser directly.
