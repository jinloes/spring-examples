# Spring gRPC Example

This project demonstrates how to build gRPC services with Spring Boot, including all four types of gRPC communication patterns.

## Features

- **Unary RPC**: Simple request-response pattern
- **Server Streaming RPC**: Server sends multiple responses to a single client request
- **Client Streaming RPC**: Client sends multiple requests and server responds once
- **Bidirectional Streaming RPC**: Both client and server send streams of messages

## Technology Stack

- Spring Boot 3.5.0
- gRPC 1.70.0
- Protocol Buffers 4.29.3
- grpc-spring-boot-starter 3.1.0.RELEASE

## Project Structure

```
grpc/
├── src/main/
│   ├── java/com/jinloes/grpc/
│   │   ├── GrpcApplication.java           # Main application
│   │   ├── client/
│   │   │   └── GreetingClient.java        # gRPC client
│   │   ├── controller/
│   │   │   └── GreetingController.java    # REST API to demo gRPC
│   │   └── service/
│   │       └── GreetingServiceImpl.java   # gRPC service implementation
│   ├── proto/
│   │   └── greeting.proto                 # Protocol Buffer definition
│   └── resources/
│       └── application.yml                # Configuration
└── src/test/
    ├── java/com/jinloes/grpc/
    │   └── GreetingServiceIntegrationTest.java
    └── resources/
        └── application-test.yml
```

## Building the Project

```bash
./gradlew :grpc:build
```

This will:
1. Compile the `.proto` files to generate Java classes
2. Build the Spring Boot application

## Running the Application

```bash
./gradlew :grpc:bootRun
```

The application will start:
- gRPC Server on port `9090`
- HTTP Server on port `8080` (default)

## Testing

Run the integration tests:

```bash
./gradlew :grpc:test
```

The tests use in-process gRPC for fast, reliable testing without network calls.

## API Endpoints

### REST API (HTTP)

These endpoints demonstrate calling gRPC services from REST controllers:

- `GET /api/greeting/{name}` - Unary call
- `GET /api/greeting/stream/server/{name}` - Server streaming
- `GET /api/greeting/stream/client` - Client streaming
- `GET /api/greeting/stream/bidirectional` - Bidirectional streaming

Example:
```bash
curl http://localhost:8080/api/greeting/World
```

### gRPC Service

The gRPC service is available on port `9090` with four methods:

1. **SayHello** (Unary)
   - Request: `HelloRequest{name: string}`
   - Response: `HelloResponse{message: string}`

2. **SayHelloServerStreaming** (Server Streaming)
   - Sends 5 greeting messages with delays

3. **SayHelloClientStreaming** (Client Streaming)
   - Collects multiple names and responds with aggregated greeting

4. **SayHelloBidirectional** (Bidirectional Streaming)
   - Echoes back greetings as they arrive

## Using gRPC Client

You can test the gRPC service using tools like:

### grpcurl

```bash
# List services
grpcurl -plaintext localhost:9090 list

# Call unary method
grpcurl -plaintext -d '{"name": "World"}' localhost:9090 greeting.GreetingService/SayHello

# Server streaming
grpcurl -plaintext -d '{"name": "Stream"}' localhost:9090 greeting.GreetingService/SayHelloServerStreaming
```

### BloomRPC or Postman

Import the `greeting.proto` file and connect to `localhost:9090`.

## Configuration

### application.yml

```yaml
grpc:
  server:
    port: 9090  # gRPC server port
  client:
    grpc-server:
      address: static://localhost:9090
      negotiation-type: plaintext
```

### For Production

For production, enable TLS:

```yaml
grpc:
  server:
    port: 9090
    security:
      enabled: true
      certificate-chain: classpath:server-cert.pem
      private-key: classpath:server-key.pem
```

## Protocol Buffer Definition

The service is defined in `src/main/proto/greeting.proto`:

```protobuf
service GreetingService {
  rpc SayHello (HelloRequest) returns (HelloResponse);
  rpc SayHelloServerStreaming (HelloRequest) returns (stream HelloResponse);
  rpc SayHelloClientStreaming (stream HelloRequest) returns (HelloResponse);
  rpc SayHelloBidirectional (stream HelloRequest) returns (stream HelloResponse);
}
```

## Key Dependencies

The `build.gradle` includes:

- `net.devh:grpc-server-spring-boot-starter` - gRPC server integration
- `net.devh:grpc-client-spring-boot-starter` - gRPC client integration
- `com.google.protobuf` plugin - Compiles `.proto` files to Java

## Learn More

- [gRPC Java Documentation](https://grpc.io/docs/languages/java/)
- [grpc-spring-boot-starter](https://github.com/grpc-ecosystem/grpc-spring)
- [Protocol Buffers](https://protobuf.dev/)