# nextjs

Demonstrates a Spring Boot backend paired with a Next.js (React) frontend. The frontend fetches
data from the Spring Boot API at startup and renders it server-side. A Next.js rewrite rule
proxies `/api/*` requests from the browser to the Spring Boot server, so no CORS configuration
is needed.

## Architecture

- **Backend** (`src/main/java/`) — Spring Boot REST API on port 8080
- **Frontend** (`frontend/`) — Next.js 16 + React 19 app on port 3000 (dev)
  - `app/page.tsx` — server component that fetches `GET /api/hello` at render time
  - `next.config.ts` — rewrites `/api/:path*` to `http://localhost:8080/api/:path*`

## How to Run

### Backend

```bash
./gradlew :nextjs:bootRun
```

Spring Boot starts on **port 8080**.

### Frontend

```bash
cd nextjs/frontend
npm install
npm run dev
```

Next.js dev server starts on **http://localhost:3000**. Open it in a browser to see the greeting
fetched from Spring Boot.

For a production build:

```bash
npm run build
npm run start   # serves on port 3000
```

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
