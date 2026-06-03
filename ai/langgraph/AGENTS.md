# ai/langgraph

Customer-support triage **agent workflow** built on **LangChain4j** + **LangGraph4j** with
**LangSmith** observability. Demonstrates how a Spring Boot service can run an LLM-driven decision
chain — classify the inquiry, dispatch to a specialised agent (each with its own tools), or loop
back for clarification — and how to wire first-class LangSmith tracing through both the graph
nodes and the underlying LLM calls.

This is the LangChain/LangGraph counterpart to the `flowable/` module's BPMN workflow — same
"decision graph with tool-calling" problem, different tech stack, different debugging story.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Profiles — picking a chat-model provider

The chat-model config lives in `application-{profile}.yml`. Two are supplied:

| Profile | Provider | When to use |
|---|---|---|
| `gemini` (default) | Google AI Studio (cloud) | Quick demos, you have a key. Free tier is tight (~20-250 RPD depending on model) — fine for showing off, painful for sustained dev. |
| `lm-studio` | LM Studio local server (OpenAI-compatible) | Sustained dev, offline, unlimited. Slower per turn but no quota. |

Switch with `SPRING_PROFILES_ACTIVE=lm-studio` (in `.env` or the IntelliJ run config) or
`--spring.profiles.active=lm-studio` on the command line. Both starters are on the classpath
simultaneously; auto-config picks the right one based on which profile's properties are set.

**LM Studio setup (manual):**

1. Open LM Studio, download a tool-calling-capable model — Qwen3-A3B variants are the most
   reliable open-source choices for this graph (e.g. `qwen/qwen3.6-35b-a3b`).
2. Click "Local Server" tab → start the server (default `http://localhost:1234`).
3. Add to your `.env`:
   ```
   SPRING_PROFILES_ACTIVE=lm-studio
   LM_STUDIO_MODEL=qwen/qwen3.6-35b-a3b
   ```
4. Start the Spring app. No `GEMINI_API_KEY` required.

**LM Studio first-call warmup:**

`config/LmStudioWarmup.java` is a `@Profile("lm-studio")`-only bean that listens for
`ApplicationReadyEvent` and fires two warmup chats against the ChatModel — one generic call to
allocate KV-cache buffers and prime the HTTP connection pool, and one classifier call to warm
llama.cpp's prompt cache for the classifier's system prompt. This moves the cold-start cost
(typically 30–90 s) from the first user message to app startup. Look for `LM Studio total
warmup time: NNN ms` in the boot log. No effect on the Gemini profile.

**LM Studio setup (automatic via Gradle):**

If you launch via `./gradlew :ai:langgraph:bootRun` from the command line with
`SPRING_PROFILES_ACTIVE=lm-studio` in your env (or in `.env`), the build script will start the
LM Studio daemon and load the model with `lms server start` + `lms load`. The load step is
idempotent — `lms ps --json` is checked first and we skip if the model is already in memory
with the right config.

Requires the `lms` CLI shim at `~/.lmstudio/bin/lms` (installed when you set up LM Studio and run
`lms bootstrap` — see https://lmstudio.ai/docs/cli for setup). With the Gemini (default)
profile, every LM Studio task is skipped via `onlyIf`, so you don't need the CLI installed.

Caveat: only runs when invoked through the Gradle CLI. IntelliJ's Spring Boot run config goes
through the main class directly, bypassing Gradle tasks. For IDE workflows either start LM
Studio manually or add `./gradlew ensureLmStudio` as a "Before Launch" step in the run config.

Expect 5–15 seconds per chat turn on Apple Silicon (vs <1s with Gemini). Tool calling on
Qwen3-A3B is solid but slightly less consistent than `gemini-2.5-flash` — if you see the LLM
calling tools without producing follow-up text, the deterministic refund path in `TriageGraph`
covers the common case anyway.

**Two non-obvious things in the LM Studio wiring** (worth knowing if you swap models):

1. **`NoThinkChatModel` prefill** — `@Profile("lm-studio")` wrapper that appends an empty
   `<think>\n\n</think>\n` assistant message to every request. llama.cpp treats the last
   assistant message as the prefix to continue from, so the model jumps straight to content
   generation instead of spending hundreds of tokens reasoning. Same trick CardFlip uses.
   Without it, "thinking" models like Qwen3.6 spend the entire response budget on
   `reasoning_content` (and the `/no_think` directive in their system prompt is silently
   ignored by Qwen3.6 specifically — verified in this repo's debugging history).

2. **Apache HttpClient5 dependency** — needed *only* for the LM Studio profile but pulled in
   unconditionally because Spring Boot's `ClientHttpRequestFactoryBuilder.detect()` picks an
   HTTP client at classpath-scan time, not per profile. Without HttpClient5, requests fall
   through to JDK 21's `java.net.http.HttpClient`, which speculatively sends
   `Upgrade: h2c, HTTP2-Settings: ...` headers on every `http://` request. LM Studio's HTTP
   server doesn't handle the h2c upgrade dance and hangs indefinitely waiting for an HTTP/2
   frame that never arrives. JDK HttpClient has no system property or builder flag to
   suppress these headers globally; HttpClient5 is HTTP/1.1 by default and never speculates.
   See the comment on the dependency in `build.gradle` for the full story.

## Run

Easiest: copy `.env.example` to `.env` and fill in your keys. Spring Boot will pick them up via
`spring.config.import: optional:file:.env[.properties]` declared in `application.yml`.

```bash
cp ai/langgraph/.env.example ai/langgraph/.env
# edit ai/langgraph/.env and set GEMINI_API_KEY=AIza...
./gradlew :ai:langgraph:bootRun
```

Or pass them as env vars instead — same outcome:

```bash
export GEMINI_API_KEY=AIza...                      # required for the LLM
export GEMINI_MODEL=gemini-2.0-flash               # optional, this is the default
export LANGSMITH_API_KEY=lsv2_...                  # optional — disables tracing if absent
export LANGSMITH_PROJECT=spring-examples-langgraph # optional, defaults to this value
./gradlew :ai:langgraph:bootRun
```

Note: `.env` is gitignored. Get a free Gemini key at https://aistudio.google.com/apikey (no
billing required).

The default model is `gemini-2.5-flash-lite` — currently the most generous free-tier model
(~30 RPM as of early 2026). A single `/triage` request fires 2–4 LLM calls (classify + the
dispatched agent + any tool follow-ups), so RPM matters more than per-call quality for
interactive chat.

If you'd rather have a slightly smarter response and can live with the ~5 RPM ceiling, set
`GEMINI_MODEL=gemini-2.5-flash` in your env / `.env`. If you see a 429 with `"limit: 0"`,
that model has been rotated off the free tier entirely — check
https://ai.google.dev/gemini-api/docs/rate-limits for the current free list and pick another.

`max-retries: 3` is set so langchain4j absorbs transient 429 bursts by honoring Gemini's
`retryDelay` header. Doesn't help if you've genuinely exhausted the per-minute cap, but smooths
over normal usage spikes.

Then open **http://localhost:8087/** for the chat UI, or POST directly:

```bash
# First turn — omit sessionId, the server generates one and returns it.
curl -s localhost:8087/triage \
  -H 'content-type: application/json' \
  -d '{"inquiry": "I want a refund"}' | jq

# Follow-up turn — pass the sessionId back to continue the same conversation.
curl -s localhost:8087/triage \
  -H 'content-type: application/json' \
  -d '{"inquiry": "ORD-1001", "sessionId": "<id-from-first-response>"}' | jq
```

Response:

```json
{
  "sessionId": "8b4f...",
  "traceId": "c019...",
  "intent": "refund",
  "response": "Your refund for ORD-1001 has been approved...",
  "clarifyCount": 0
}
```

## Chat UI

`src/main/resources/static/index.html` — single-file vanilla JS chat client:

- Stores the `sessionId` returned by the first response and includes it on every subsequent
  request, giving you a real multi-turn conversation with the agents' shared memory backing.
- Renders bot replies with an intent badge (`refund`/`technical`/`billing`/`clarify`/`escalate`)
  and a short trace-id stub so you can locate the corresponding LangSmith run.
- "New conversation" button resets the JS-side session id, causing the next message to start a
  fresh server-side memory (the old memory is orphaned in `InMemoryChatMemoryStore` until process
  restart — switch to a persistent store like `langchain4j-store-memory-chat-redis` for prod).

## Test

```bash
./gradlew :ai:langgraph:test
```

`TriageGraphTest` boots the full Spring context with `@MockitoBean`-replaced agent beans and
verifies:

- `refund` / `technical` / `billing` intents each route to the corresponding handler.
- The clarify branch loops back to classify, and after `MAX_CLARIFY_TURNS=2` switches to
  `intent=escalate` and writes an escalation message into `response`.

Tests do not call OpenAI or LangSmith (`langsmith.enabled=false` and all agents are mocked).

## Dependencies

| Artifact | Version | Why |
|---|---|---|
| `dev.langchain4j:langchain4j-bom` | 1.15.0 | Pins the langchain4j core to the latest stable (otherwise `langgraph4j-langchain4j` would pull 1.14.1 transitively). |
| `dev.langchain4j:langchain4j-google-ai-gemini-spring-boot4-starter` | 1.15.0-beta25 | Spring Boot 4 starter for Google AI Studio — used when the `gemini` profile is active. |
| `dev.langchain4j:langchain4j-open-ai-spring-boot4-starter` | 1.15.0-beta25 | Spring Boot 4 starter for any OpenAI-compatible API — used when the `lm-studio` profile is active. Both starters are on the classpath simultaneously; only one creates a `ChatModel` bean because auto-config keys on the active profile's api-key property. |
| `org.apache.httpcomponents.client5:httpclient5` | (Spring-managed) | Required for LM Studio / any local OpenAI-compatible server. Spring Boot 4's `ClientHttpRequestFactoryBuilder.detect()` picks this over the JDK HttpClient when both are on the classpath; the JDK client speculatively sends `Upgrade: h2c` headers that LM Studio hangs on indefinitely. See the comment in `build.gradle` for the full story. |
| `org.bsc.langgraph4j:langgraph4j-bom` | 1.8.17 | Pins `langgraph4j-core` and `langgraph4j-langchain4j`. |
| `org.bsc.langgraph4j:langgraph4j-core` | 1.8.17 | Framework-agnostic state-graph runtime. |
| `org.bsc.langgraph4j:langgraph4j-langchain4j` | 1.8.17 | Optional helpers — not strictly required here, but useful if you reach for `MessagesState`, `LC4jToolService`, or the prebuilt ReAct agent. |

The `langchain4j-spring-bom` is intentionally NOT imported — it has not been updated since
`1.0.0-beta5` (May 2025) and would not manage the current Spring Boot 4 starter versions.
