# grpc/hello-world

Demonstrates all four gRPC communication patterns (unary, server streaming, client streaming,
bidirectional streaming) using a `GreetingService`, with a REST facade that exercises each pattern.

## How to Run

```bash
./gradlew :grpc:hello-world:bootRun
```

gRPC server: **9090**, HTTP server: **8080**

## API Endpoints

| Method | Path | gRPC pattern |
|---|---|---|
| `GET` | `/api/greeting/{name}` | Unary |
| `GET` | `/api/greeting/stream/server/{name}` | Server streaming |
| `GET` | `/api/greeting/stream/client` | Client streaming |
| `GET` | `/api/greeting/stream/bidirectional` | Bidirectional streaming |

```bash
curl http://localhost:8080/api/greeting/World
```

## Proto (`src/main/proto/greeting.proto`)

```protobuf
service GreetingService {
  rpc SayHello (HelloRequest) returns (HelloResponse);
  rpc SayHelloServerStreaming (HelloRequest) returns (stream HelloResponse);
  rpc SayHelloClientStreaming (stream HelloRequest) returns (HelloResponse);
  rpc SayHelloBidirectional (stream HelloRequest) returns (stream HelloResponse);
}
```

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
