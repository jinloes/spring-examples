# ai/todo-mcp

Demonstrates Spring AI's Model Context Protocol (MCP) with a todo list domain. The server exposes CRUD todo operations as MCP tools; the client connects to the server via SSE and uses an Ollama-backed chat model to invoke those tools in natural language.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Running

Start the server first, then the client.

```bash
# Terminal 1 — MCP server
./gradlew :ai:todo-mcp:server:bootRun

# Terminal 2 — MCP client (requires Ollama running locally)
./gradlew :ai:todo-mcp:client:bootRun
```

Ollama model is configured in `client/src/main/resources/application.yml` (`gpt-oss:20b`). Change it to any locally available model.

## Testing

```bash
./gradlew :ai:todo-mcp:server:test
```

Tests cover all REST endpoints in `TodoControllerTest` using `MockMvcTester` with a mocked `TodoRepository`.

## Spring AI Version

Both server and client use **Spring AI 1.1.2** (`spring-ai-bom:1.1.2`).
