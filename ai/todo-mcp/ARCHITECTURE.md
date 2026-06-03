# ai/todo-mcp — Architecture

Reference and design details for `ai/todo-mcp`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
