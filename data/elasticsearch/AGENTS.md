# data/elasticsearch

Demonstrates Spring Data Elasticsearch — index creation, document persistence, and REST-based search/retrieval using an `ElasticsearchRepository`.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

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

## Test

Tests use Testcontainers — Docker must be running.

```bash
./gradlew :data:elasticsearch:test
```
