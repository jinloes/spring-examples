# datasketches

Spring Boot web analytics API demonstrating four [Apache DataSketches](https://datasketches.apache.org/)
sketch types for approximate, memory-efficient analytics over event streams.

## How to Run

```bash
./gradlew :datasketches:bootRun
```

## API

| Method | Path | Sketch | Description |
|---|---|---|---|
| `POST` | `/api/events` | all | Record an analytics event |
| `GET` | `/api/analytics` | all | Full analytics snapshot |
| `GET` | `/api/analytics/unique-visitors` | Theta | Estimated unique visitor count + bounds |
| `GET` | `/api/analytics/unique-pages` | HLL | Estimated unique page count + bounds |
| `GET` | `/api/analytics/response-times` | Quantiles | p50 / p95 / p99 response times |
| `GET` | `/api/analytics/popular-pages?limit=N` | Frequency | Top-N most visited pages |
| `DELETE` | `/api/analytics` | all | Reset all sketches |

### Event payload

```json
{ "userId": "user-123", "pageUrl": "/home", "responseTimeMs": 142.5 }
```

All fields are optional — null fields are silently skipped, so partial events are fine.

## Sketch Overview

### Theta sketch — `GET /api/analytics/unique-visitors`

Maintains a random sample of hashed values with a dynamic threshold (theta). As N grows,
theta shrinks to keep the sample at a fixed size. Estimate = sample size / theta.

Key advantage over HLL: **supports set operations**. Two Theta sketches can be unioned or
intersected without replaying raw data, making it ideal for combining daily sketches into
weekly unique counts.

Response includes 95% confidence bounds (`lowerBound`, `upperBound`).

### HLL sketch — `GET /api/analytics/unique-pages`

Uses a fixed-size register array (2^lgK entries) to track leading-zero patterns in hashed
values. At lgK=12, the sketch uses ~5 KB of memory regardless of cardinality and achieves
~0.8% relative standard error.

More memory-efficient than Theta when set operations are not needed.

### Quantiles sketch — `GET /api/analytics/response-times`

Maintains a compact mergeable summary structure that answers arbitrary rank/quantile queries
in O(log N) space. Rank error is bounded by 1/(2*k). No need to store all response times.

### Frequency sketch — `GET /api/analytics/popular-pages`

Implements the space-saving algorithm. Pages appearing above the error threshold are
guaranteed to appear in the result (`NO_FALSE_NEGATIVES`). The `maxMapSize` parameter trades
memory for accuracy.

## Example Workflow

```bash
# Record some events
curl -X POST http://localhost:8080/api/events \
  -H 'Content-Type: application/json' \
  -d '{"userId":"alice","pageUrl":"/home","responseTimeMs":120}'

curl -X POST http://localhost:8080/api/events \
  -H 'Content-Type: application/json' \
  -d '{"userId":"bob","pageUrl":"/about","responseTimeMs":340}'

# Query analytics
curl http://localhost:8080/api/analytics

# Individual sketch queries
curl http://localhost:8080/api/analytics/unique-visitors
curl http://localhost:8080/api/analytics/response-times
curl http://localhost:8080/api/analytics/popular-pages?limit=5

# Reset
curl -X DELETE http://localhost:8080/api/analytics
```

## Tests

```bash
./gradlew :datasketches:test
```

- `SketchServiceTest` — unit tests directly against `SketchService`: accuracy assertions for
  large N, null-field handling, reset, confidence bound ordering
- `AnalyticsControllerTest` — `@WebMvcTest` slice with mocked service covering all endpoints
