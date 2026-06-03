# data/elasticsearch — Architecture

Reference and design details for `data/elasticsearch`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## What it shows

- `@Document` on a Java record mapped to an Elasticsearch index
- `ElasticsearchRepository` for CRUD operations
- `@ResponseStatus(NOT_FOUND)` exception for clean 404 responses
- Integration tests with Testcontainers (`ElasticsearchContainer`) and `@DynamicPropertySource`

## API Endpoints

| Method | Path           | Body                  | Description             |
|--------|----------------|-----------------------|-------------------------|
| POST   | `/alarms`      | `{ "org": <int> }`    | Create a new alarm      |
| GET    | `/alarms/{id}` |                       | Fetch alarm by ID (404 if not found) |

### Examples

```bash
# Create an alarm
curl -s -X POST http://localhost:8080/alarms \
  -H 'Content-Type: application/json' \
  -d '{"org": 42}'

# Fetch an alarm
curl -s http://localhost:8080/alarms/<id>
```
