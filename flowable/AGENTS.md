# flowable

Spring Boot + Flowable BPM example demonstrating a **loan approval workflow** using BPMN 2.0.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :flowable:bootRun
```

On first run, Gradle downloads Node 20 (via `gradle-node-plugin`) and builds the React frontend
with Vite. Subsequent runs use the cached build.

- **UI**: `http://localhost:8080/` — React single-page app (three tabs: Apply, Check Status, Manager Tasks)
- **API base**: `http://localhost:8080/loans`
- **H2 console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:flowabledb`)

### Frontend development

For live-reloading during frontend work, run the Vite dev server alongside Spring Boot:

```bash
# Terminal 1 — Spring Boot API
./gradlew :flowable:bootRun

# Terminal 2 — Vite dev server (proxies /loans → localhost:8080)
cd flowable/frontend
npm install
npm run dev   # http://localhost:5173
```

## Frontend

React + Vite SPA in `frontend/`. Built by Gradle and served as static resources from the JAR.

| File | Purpose |
|------|---------|
| `frontend/src/App.jsx` | Three-tab UI: Apply, Check Status, Manager Tasks |
| `frontend/src/App.css` | Styles |
| `frontend/vite.config.js` | Vite config with `/loans` proxy for local dev |
| `build.gradle` — `installFrontend` / `buildFrontend` tasks | Gradle integration via `gradle-node-plugin` (downloads Node 20) |

## Tests

```bash
./gradlew :flowable:test
```

Five tests cover: auto-approve, auto-reject, user-task creation, manager approve, manager reject.
