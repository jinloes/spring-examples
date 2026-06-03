# loom-web — Architecture

Reference and design details for `web/loom-web`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Endpoints

| Endpoint | Description |
|---|---|
| `GET /threads/info` | Returns current thread name, ID, and whether it is virtual |
| `GET /threads/blocking?delayMs=1000` | Sleeps for `delayMs` ms — simulates a blocking I/O call |
| `GET /threads/fanout?tasks=10&delayMs=500` | Fans out `tasks` parallel blocking calls via a virtual thread executor |
| `GET /threads/memory` | Returns JVM heap and process RSS in MB |

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
