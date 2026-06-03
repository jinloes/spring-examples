# ai/langgraph — Architecture

Reference and design details for `ai/langgraph`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Backend-driven UI actions

The refund agent demonstrates a pattern that goes beyond text-only chat: a tool can queue a
**UI action** that the chat client renders as clickable buttons.

Flow when the customer says "I want a refund" without an order id:

1. Refund agent's LLM decides to call `RefundTool.listAvailableOrders()`.
2. The tool reads the current `sessionId` from {@link SessionContext} (a `ThreadLocal` set by
   the refund graph node), and queues a `UiAction` into {@link PendingActionsRegistry}.
3. The agent's text response is brief ("Sure — which order?") because the prompt tells it not
   to enumerate ids; the UI handles that.
4. `TriageController` drains the registry for that session and includes `actions` in the
   `TriageResponse`.
5. The chat UI renders each option as a button; clicking one submits the order id as the next
   user message.

Key files:
- `web/UiAction.java` — the response payload type (`type`, `prompt`, `options`).
- `observability/SessionContext.java` — `ThreadLocal<sessionId>` so tools deep inside the
  `AiServices` proxy know which session they're in without changing the tool's `@Tool`
  signature (which would alter the prompt the LLM sees).
- `observability/PendingActionsRegistry.java` — per-session buffer; tools enqueue, controller
  drains.
- `tools/RefundTool.java` — `listAvailableOrders()` is the tool that uses the pattern.

This is a small backend-driven dynamic UI demo, akin to `grpc/react-dynamic-components/` but
for an LLM agent. Add more action types (`select_account`, `confirm_charge`, etc.) by extending
`UiAction.type` and adding the matching renderer in `index.html`.

## Chat memory

Each agent has its own `ChatMemoryProvider` namespaced by agent (`refund:{sessionId}`,
`technical:{sessionId}`, ...) but all share one `InMemoryChatMemoryStore` bean. Why per-agent
namespacing instead of one shared memory:

- Every `AiServices` invocation appends the user message to memory before sending. If the
  classifier and a response agent both used the same memory, each turn would duplicate the user
  message in history.
- The classifier carrying its own memory is intentional: on a follow-up like "ORD-1234", the
  classifier needs to recall the prior "refund" classification to route correctly. Without
  memory, the classifier would see only "ORD-1234" in isolation and pick `clarify`.

Tradeoff: if a customer switches intent mid-conversation (refund → billing), the new agent has
fresh memory and doesn't see the refund context. That's the conventional "transferred to a new
specialist" UX. Cross-agent context-sharing would need a higher-level conversation log injected
into each agent's system prompt — out of scope here.

If `LANGSMITH_API_KEY` was set, open https://smith.langchain.com → your project → the run with the
returned `traceId`. You'll see:

- The root `support_triage` span (whole request).
  - The `classify_intent` node span.
    - The `gemini-2.0-flash` LLM call inside it (prompts, completion, tokens).
  - The dispatched node span (`refund` / `technical` / `billing` / `clarify`).
    - The LLM call(s) inside it, including tool invocations.

## Architecture

```
                 ┌──────────────────────────┐
POST /triage ──▶ │   TriageController       │
                 │  (begins LangSmith trace)│
                 └─────────────┬────────────┘
                               │  invoke(...)
                               ▼
                     ┌─────────────────┐
                     │ CompiledGraph   │  ◀── built by TriageGraph @Configuration
                     │ <TriageState>   │
                     └────────┬────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        │             classify_intent               │  IntentClassifier (AiServices)
        └────┬──────────┬───────────┬───────────┬───┘
             │          │           │           │
          refund    technical    billing     clarify
             │          │           │           │
     RefundAgent   TechnicalAgent BillingAgent  ClarifyAgent
     + RefundTool  + DocsSearch   + AccountTool (lists supported topics)
             │          │           │           │
             └──────────┴───────────┴───────────┴────▶ END
```

Each `/triage` call handles exactly one user turn. The cross-turn cycle — agent asks for an order
id, user types it, agent uses it — comes from the chat client re-POSTing with the same
`sessionId`, not from a loop inside the graph. The graph's cycles would be re-classifying the
same unchanged input and burning LLM calls.

### Key components

| File | Role |
|---|---|
| `graph/TriageState.java` | Extends `AgentState`; holds `inquiry`, `intent`, `response`, `sessionId`, `traceId`. |
| `graph/TriageGraph.java` | `@Configuration` that builds the `StateGraph` and compiles it. Defines the conditional edges that route on `state.intent()`. |
| `nodes/IntentClassifier.java` etc. | Plain interfaces; turned into LLM-backed proxies by `AgentsConfig` via `AiServices.builder()`. |
| `tools/RefundTool.java` etc. | `@Tool`-annotated methods. Wired to specific agents in `AgentsConfig`. |
| `observability/LangSmithClient.java` | Posts completed `Run` objects to LangSmith via REST. |
| `observability/LangSmithChatModelListener.java` | Implements `ChatModelListener`; the langchain4j starter auto-discovers it and instruments every chat completion. |
| `observability/LangSmithGraphHooks.java` | Attaches `BeforeCall` / `AfterCall` hooks to the graph so each node execution becomes a parent span for the LLM calls inside it. |
| `observability/LangSmithTraceContext.java` | Per-request `ThreadLocal` stack: trace id + current parent run id. |
| `web/TriageController.java` | Begins/ends the root LangSmith trace; calls `compiledGraph.invoke(...)`. |

### LangGraph4j pitfalls hit while building this

- **`addAfterCallNodeHook` return value is NOT ignored.** It's reduced into the partial-state map
  that gets merged into the graph state. If you return `Map.of()` you silently overwrite the
  node's own partial result and downstream fields (like `intent`) disappear. `LangSmithGraphHooks`
  returns `lastResult` unchanged to avoid this.
- **`SCHEMA = Map.of()` is fine for plain overwrite semantics.** State keys not in the schema
  default to last-write-wins. You only need a channel when you want appender / custom merge.
- **`@Configuration` class name collides with `@Bean` method name.** The class `TriageGraph` is
  itself a bean named `triageGraph` — its `@Bean` method must use a different name
  (`compiledTriageGraph`).
- **`@MockBean` is gone in Spring Boot 4** — use `@MockitoBean` from
  `org.springframework.test.context.bean.override.mockito`.

## Observability

### Why LangSmith

LangSmith is the LangChain ecosystem's purpose-built trace viewer. Every LLM call shows
prompts/completions, token counts, latency; every graph node becomes a child span. The trace tree
mirrors the graph execution tree exactly.

### How runs are correlated

- `TriageController` mints a `traceId` and `rootRunId` per request and calls
  `LangSmithTraceContext.beginTrace(...)`.
- Each graph node's `BeforeCall` hook pushes a fresh `nodeRunId` onto the trace's parent stack.
- `LangSmithChatModelListener` reads `currentParent()` on each LLM call so the LLM run is attached
  to the right node.
- The `AfterCall` hook pops the stack and posts the completed node `Run` to LangSmith.

Because we use synchronous `node_async(syncNode)` wrappers — which execute on the calling thread —
`ThreadLocal` is sufficient for context propagation. If you switch to genuinely async nodes that
hop threads (CompletableFuture chained across pools), replace `LangSmithTraceContext` with
`RunnableConfig` metadata.

### Disabling LangSmith

Either don't set `LANGSMITH_API_KEY`, or set `LANGSMITH_ENABLED=false`. The
`LangSmithClient.postCompletedRun(...)` becomes a no-op and the graph runs normally.

### Alternative: OpenTelemetry

LangGraph4j ships `langgraph4j-opentelemetry`. Combined with langchain4j's `ChatModelListener` and
an OTLP exporter, you can send the same trace tree to **any** OTel-compatible backend (Jaeger,
Tempo, Datadog, Honeycomb, or LangSmith itself — they accept OTLP). The REST-based approach in
this module is simpler to read; switch to OTLP if you already run an OTel collector.
