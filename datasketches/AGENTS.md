# datasketches

Spring Boot web analytics API demonstrating four [Apache DataSketches](https://datasketches.apache.org/)
sketch types for approximate, memory-efficient analytics over event streams.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :datasketches:bootRun
```

## Tests

```bash
./gradlew :datasketches:test
```

- `SketchServiceTest` — unit tests directly against `SketchService`: accuracy assertions for
  large N, null-field handling, reset, confidence bound ordering
- `AnalyticsControllerTest` — `@WebMvcTest` slice with mocked service covering all endpoints
