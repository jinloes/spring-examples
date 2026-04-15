# ai/todo-mcp

Demonstrates Spring AI's Model Context Protocol (MCP) with a todo list domain. The server exposes CRUD todo operations as MCP tools; the client connects to the server via SSE and uses an Ollama-backed chat model to invoke those tools in natural language.

## Architecture

```
client (Ollama LLM + MCP client)
    │  SSE connection
    ▼
server (Spring MVC + MCP server, port 8081)
    ├── TodoController  — REST API  (/api/todos)
    ├── TodoService     — @Tool-annotated methods registered as MCP tools
    ├── TodoRepository  — in-memory ConcurrentHashMap store
    └── McpConfig       — registers TodoService tools via MethodToolCallbackProvider
```

### Server

- Spring Boot 4 + `spring-ai-starter-mcp-server-webmvc`
- `TodoService` methods are annotated with `@Tool` / `@ToolParam` and registered as MCP tools via `MethodToolCallbackProvider`
- Also exposes a standard REST API so the tools can be exercised without an LLM
- Runs on port **8081**

### Client

- Spring Boot 4 + `spring-ai-starter-mcp-client` + `spring-ai-starter-model-ollama`
- Connects to the server over SSE (`http://localhost:8081`)
- `TodoAssistant` builds a `ChatClient` with the MCP tool callbacks and accepts free-text prompts
- On startup, `CommandLineRunner` sends a demo prompt to create and list todos

## Running

Start the server first, then the client.

```bash
# Terminal 1 — MCP server
./gradlew :ai:todo-mcp:server:bootRun

# Terminal 2 — MCP client (requires Ollama running locally)
./gradlew :ai:todo-mcp:client:bootRun
```

Ollama model is configured in `client/src/main/resources/application.yml` (`gpt-oss:20b`). Change it to any locally available model.

## REST Endpoints (server)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/todos` | List all todos |
| GET | `/api/todos/{id}` | Get a todo by ID |
| POST | `/api/todos` | Create a todo (`{"title":"...", "description":"..."}`) |
| PUT | `/api/todos/{id}` | Update title/description |
| PATCH | `/api/todos/{id}/complete` | Mark as completed |
| DELETE | `/api/todos/{id}` | Delete a todo |

## MCP Tools (server)

| Tool | Description |
|------|-------------|
| `createTodo` | Create a new todo item |
| `getAllTodos` | List all todo items |
| `getTodoById` | Fetch a single todo by ID |
| `updateTodo` | Update title and description |
| `completeTodo` | Mark a todo as completed |
| `deleteTodo` | Delete a todo |

## Testing

```bash
./gradlew :ai:todo-mcp:server:test
```

Tests cover all REST endpoints in `TodoControllerTest` using `MockMvcTester` with a mocked `TodoRepository`.

## Spring AI Version

Both server and client use **Spring AI 1.1.2** (`spring-ai-bom:1.1.2`).
