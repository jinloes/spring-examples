# web/http2

Demonstrates HTTP/2 with Spring Boot using h2c (HTTP/2 Cleartext — no TLS required). A single
property enables HTTP/2 support; Tomcat negotiates the h2c upgrade automatically.

## How to Run

```bash
./gradlew :web:http2:bootRun
```

Server starts on **port 8080**.

## Configuration

HTTP/2 is enabled in `src/main/resources/application.yaml`:

```yaml
server:
  http2:
    enabled: true
```

h2c allows HTTP/2 over a plain (non-TLS) connection, which is useful for local development and
internal services where TLS termination happens at the load balancer.

## Endpoint

| Method | Path | Description |
|---|---|---|
| `GET` | `/hello` | Returns the string `hello` |

## Testing with curl

Use `--http2` to send an HTTP/2 request. The `-sI` flags print response headers only (silent
output, show headers), where you can confirm `HTTP/2 200`:

```bash
# Check headers to confirm HTTP/2 negotiation
curl --http2 -sI http://localhost:8080/hello

# Get the response body
curl --http2 http://localhost:8080/hello
```

Expected header output includes `HTTP/2 200`, confirming the protocol upgrade succeeded.
