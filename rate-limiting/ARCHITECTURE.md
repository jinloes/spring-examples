# rate-limiting — Architecture

Reference and design details for `rate-limiting`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Key classes

| Class | Description |
|-------|-------------|
| `SlidingWindowRateLimiterService` | Sliding window rate limiter backed by Redis sorted set + Lua |
| `Bucket4JRateLimiter` | Token bucket rate limiter via Bucket4j + Redis (Lettuce) |
| `RateLimitResult` | Immutable record: `allowed`, `remainingTokens`, `retryAfter` |
| `RedisConfig` | Configures `RedisTemplate` and Bucket4j `ProxyManager` |
