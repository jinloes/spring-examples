# grpc/hello-world — Architecture

Reference and design details for `grpc/hello-world`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
