# data/elasticsearch

Demonstrates Spring Data Elasticsearch — index creation, document persistence, and REST-based search/retrieval using an `ElasticsearchRepository`.

## What it shows

- `@Document` on a Java record mapped to an Elasticsearch index
- `ElasticsearchRepository` for CRUD operations
- `@ResponseStatus(NOT_FOUND)` exception for clean 404 responses
- Integration tests with Testcontainers (`ElasticsearchContainer`) and `@DynamicPropertySource`

## Requirements

A running Elasticsearch instance is required (version 9.x). The app reads `ELASTICSEARCH_URI` (default: `http://localhost:9200`).

Start Elasticsearch locally with Docker:

```bash
docker run -d --name es \
  -p 9200:9200 \
  -e "xpack.security.enabled=false" \
  -e "discovery.type=single-node" \
  docker.elastic.co/elasticsearch/elasticsearch:9.0.0
```

## Run

```bash
./gradlew :data:elasticsearch:bootRun
```

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

## Test

Tests use Testcontainers — Docker must be running.

```bash
./gradlew :data:elasticsearch:test
```
