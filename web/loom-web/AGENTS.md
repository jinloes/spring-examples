# loom-web

> **For Claude**: Keep this file up to date whenever you change this module. Specifically:
> - Update the endpoints table when adding/removing/changing controller methods
> - Update the profiles table when changing `application-*.yaml` files
> - Update the k6 scenarios table when changing `stress/load.js` options
> - Update the "What We've Observed" section when the test setup changes in a way that affects expected results

Demonstrates Project Loom virtual threads in a Spring MVC application and their effect on
throughput and memory compared to platform (OS) threads.

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

## Endpoints

| Endpoint | Description |
|---|---|
| `GET /threads/info` | Returns current thread name, ID, and whether it is virtual |
| `GET /threads/blocking?delayMs=1000` | Sleeps for `delayMs` ms — simulates a blocking I/O call |
| `GET /threads/fanout?tasks=10&delayMs=500` | Fans out `tasks` parallel blocking calls via a virtual thread executor |
| `GET /threads/memory` | Returns JVM heap and process RSS in MB |

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

## What We've Observed

### Throughput
Virtual threads handle 1000 concurrent blocking requests with ~1s p95 latency because each
request gets its own lightweight virtual thread. Platform threads with a 1000-thread pool
behave similarly at this concurrency level since the pool is large enough — the real platform
thread advantage disappears when the pool saturates (e.g., capped at 200 threads vs 1000 VUs).

### Memory (RSS)
The key comparison is at equal concurrency (both profiles allow 1000 concurrent requests):

- **Platform threads**: 1000 OS threads × ~512KB native stack = ~500MB additional native RSS
- **Virtual threads**: ~8–16 carrier threads (= CPU cores) + virtual thread stack chunks on heap

Virtual thread stacks are stored on the JVM heap as small "stack chunk" objects rather than
as fixed native OS memory. At 1000 concurrent virtual threads the heap usage is higher than
idle, but total RSS (heap + native) is lower than 1000 OS threads because:
- Native thread stacks are gone (only carrier threads remain)
- Virtual thread stack chunks are small and GC-collectable

The memory gap widens over longer test runs (60s) as the JVM GC collects dead stack chunks
and the native OS thread stack cost for platform threads accumulates.

### Fan-out
The `fanout` endpoint shows virtual threads' concurrency model clearly: 10 parallel tasks
per request, each blocking for 500ms, complete in ~500ms total because each task runs on its
own virtual thread. With a fixed platform thread pool this pattern would exhaust threads
quickly under load.