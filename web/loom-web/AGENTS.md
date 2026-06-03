# loom-web

> **For Claude**: Keep this file up to date whenever you change this module. Specifically:
> - Update the endpoints table when adding/removing/changing controller methods
> - Update the profiles table when changing `application-*.yaml` files
> - Update the k6 scenarios table when changing `stress/load.js` options
> - Update the "What We've Observed" section when the test setup changes in a way that affects expected results

Demonstrates Project Loom virtual threads in a Spring MVC application and their effect on
throughput and memory compared to platform (OS) threads.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

### Start the app manually

```bash
# Virtual threads (default carrier threads = CPU cores)
./gradlew :web:loom-web:bootRun --args='--spring.profiles.active=virtual'

# Platform threads (1000-thread Tomcat pool)
./gradlew :web:loom-web:bootRun --args='--spring.profiles.active=platform'
```

### Run the k6 stress test

Requires [k6](https://k6.io/) — the Gradle tasks will install it via Homebrew if missing.

```bash
# Start app + run k6 with virtual threads profile
./gradlew :web:loom-web:k6Virtual

# Start app + run k6 with platform threads profile
./gradlew :web:loom-web:k6Platform

# Run k6 against an already-running app
./gradlew :web:loom-web:k6
```

## Spring Profiles

| Profile | Threads | `tomcat.threads.max` |
|---|---|---|
| `virtual` | Virtual threads via `spring.threads.virtual.enabled=true` | Ignored (virtual thread executor replaces the pool) |
| `platform` | OS platform threads | `1000` (matches k6 VU count for fair comparison) |

## k6 Stress Test Scenarios

Defined in `stress/load.js`:

| Scenario | VUs | Duration | What it tests |
|---|---|---|---|
| `blocking` | 1000 | 60s | 1000 concurrent 1s blocking calls |
| `fanout` | 50 | 60s (starts at 65s) | 10 parallel 500ms calls per request |
| `memoryPoller` | 1 | 125s | Samples `/threads/memory` every second |
