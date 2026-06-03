# jobrunr

Spring Boot 4.0.5 + JobRunr 8.5.2 example demonstrating **background job processing** — fire-and-forget, delayed, and recurring jobs with the built-in dashboard.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :jobrunr:bootRun
```

- **API**: `http://localhost:8080/jobs`
- **JobRunr dashboard**: `http://localhost:8000` — shows enqueued, processing, succeeded, and failed jobs in real time
- **H2 console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:jobrunrdb`)

## Recurring job

A cleanup job is registered on startup via `JobScheduler.scheduleRecurrently()` with cron `* * * * *` (every minute, for demo purposes). It calls `ReportService::cleanupOldReports`. In a real application you would use a less frequent cron expression.

## Tests

```bash
./gradlew :jobrunr:test
```

Three integration tests verify that each endpoint enqueues a job and returns a non-blank job ID.
