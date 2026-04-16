# jobrunr

Spring Boot 4.0.5 + JobRunr 8.5.2 example demonstrating **background job processing** — fire-and-forget, delayed, and recurring jobs with the built-in dashboard.

## What is JobRunr?

[JobRunr](https://www.jobrunr.io/) is a distributed background job scheduler for Java. It persists jobs to a relational database, retries failed jobs with exponential back-off, and ships a real-time dashboard to monitor job state.

### Core job types

| Type | When to use |
|------|-------------|
| **Fire-and-forget** | Offload work from the request thread (email, report generation) |
| **Delayed** | Schedule a job to run at a future point in time |
| **Recurring** | Cron-driven jobs (cleanup, nightly exports) |

### How JobRunr executes jobs

JobRunr introspects the lambda passed to `JobScheduler.enqueue()` / `schedule()`, serializes the method name and arguments to the backing store, then a background `BackgroundJobServer` thread pool picks it up and invokes the method on a Spring-managed bean. This means job arguments must be JSON-serializable, and the method must be on a Spring bean.

## How to Run

```bash
./gradlew :jobrunr:bootRun
```

- **API**: `http://localhost:8080/jobs`
- **JobRunr dashboard**: `http://localhost:8000` — shows enqueued, processing, succeeded, and failed jobs in real time
- **H2 console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:jobrunrdb`)

## Endpoints

### Enqueue a report (fire-and-forget)
```
POST /jobs/report
Content-Type: application/json

{ "reportType": "MONTHLY" }
```

### Schedule a report for later
```
POST /jobs/report/scheduled
Content-Type: application/json

{ "reportType": "WEEKLY", "delaySeconds": 30 }
```

### Enqueue an email notification
```
POST /jobs/email
Content-Type: application/json

{
  "recipient": "user@example.com",
  "subject": "Report Ready",
  "body": "Your report is available."
}
```

All endpoints return:
```json
{ "jobId": "<uuid>", "message": "..." }
```

Use the `jobId` to look up the job in the dashboard.

## Recurring job

A cleanup job is registered on startup via `JobScheduler.scheduleRecurrently()` with cron `* * * * *` (every minute, for demo purposes). It calls `ReportService::cleanupOldReports`. In a real application you would use a less frequent cron expression.

## Key Classes

| Class | Role |
|-------|------|
| `ReportService` | Job methods — `generateReport`, `sendEmailNotification`, `cleanupOldReports`; `@Job(name=...)` sets the display name in the dashboard |
| `JobController` | REST API — enqueue/schedule jobs and return their IDs |
| `JobRunrApplication` | Registers the recurring cleanup job on startup |

## Tests

```bash
./gradlew :jobrunr:test
```

Three integration tests verify that each endpoint enqueues a job and returns a non-blank job ID.