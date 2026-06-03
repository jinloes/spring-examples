# nextjs

Demonstrates a Spring Boot backend paired with a Next.js (React) frontend. The frontend fetches
data from the Spring Boot API at startup and renders it server-side. A Next.js rewrite rule
proxies `/api/*` requests from the browser to the Spring Boot server, so no CORS configuration
is needed.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

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
