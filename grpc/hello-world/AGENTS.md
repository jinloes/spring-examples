# grpc/hello-world

Demonstrates all four gRPC communication patterns (unary, server streaming, client streaming,
bidirectional streaming) using a `GreetingService`, with a REST facade that exercises each pattern.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :grpc:hello-world:bootRun
```

gRPC server: **9090**, HTTP server: **8080**

## grpcurl examples

```bash
grpcurl -plaintext -d '{"name": "World"}' localhost:9090 greeting.GreetingService/SayHello
grpcurl -plaintext -d '{"name": "Stream"}' localhost:9090 greeting.GreetingService/SayHelloServerStreaming
```

## Tests

```bash
./gradlew :grpc:hello-world:test
```

Uses in-process gRPC — no real network calls.
