# rate-limiting

Demonstrates two Redis-backed rate-limiting strategies in a Spring Boot application:

1. **Sliding window** (`SlidingWindowRateLimiterService`) — uses a Redis sorted set and a Lua script
   (`rate_limit.lua`) to count requests within a rolling time window. Atomic Lua execution
   guarantees correctness under concurrent load.

2. **Token bucket via Bucket4j** (`Bucket4JRateLimiter`) — uses the Bucket4j library with a
   Lettuce-backed `ProxyManager` to maintain per-key token buckets in Redis. Supports configurable
   capacity, refill rate, and refill period with several convenience methods for common scenarios.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to run

Requires a running Redis instance (default: `localhost:6379`).

```bash
./gradlew :rate-limiting:bootRun
```

## Configuration properties

```yaml
spring:
  data:
    redis:
      host: localhost   # default
      port: 6379        # default
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

## How to test

Tests use Testcontainers to spin up a real Redis instance automatically — no local Redis needed.

```bash
./gradlew :rate-limiting:test
```
