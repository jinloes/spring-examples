package com.jinloes.langgraph.observability;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * ChatModelListener that posts every LLM call to LangSmith as a {@code "llm"} run, attributed to
 * the currently-executing graph node via {@link LangSmithTraceContext}.
 *
 * <p>The langchain4j Spring Boot starter auto-discovers beans of type {@link ChatModelListener} and
 * wires them onto the auto-configured {@code OpenAiChatModel}, so simply registering this as a bean
 * is enough to instrument every chat completion.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LangSmithChatModelListener implements ChatModelListener {

  /** Map keys used to stash per-call state in the listener's attributes map. */
  private static final Object START_TIME = new Object();

  private static final Object RUN_ID = new Object();
  private static final Object PARENT = new Object();
  private static final Object TRACE_ID = new Object();

  private final LangSmithClient langSmith;
  private final LangSmithTraceContext traceContext;

  @Override
  public void onRequest(ChatModelRequestContext ctx) {
    UUID traceId = traceContext.currentTraceId();
    if (traceId == null) {
      // No active trace — this LLM call is outside any /triage request (e.g. the LM Studio
      // warmup at startup). Don't try to record it; ctx.attributes() is a ConcurrentHashMap and
      // would NPE on a null parent. onResponse / onError handle missing attributes safely.
      return;
    }
    UUID runId = langSmith.newRunId();
    UUID parent = traceContext.currentParent();
    ctx.attributes().put(RUN_ID, runId);
    ctx.attributes().put(START_TIME, Instant.now());
    if (parent != null) {
      ctx.attributes().put(PARENT, parent);
    }
    ctx.attributes().put(TRACE_ID, traceId);
  }

  @Override
  public void onResponse(ChatModelResponseContext ctx) {
    UUID runId = (UUID) ctx.attributes().get(RUN_ID);
    Instant start = (Instant) ctx.attributes().get(START_TIME);
    UUID parent = (UUID) ctx.attributes().get(PARENT);
    UUID traceId = (UUID) ctx.attributes().get(TRACE_ID);
    if (runId == null || start == null) {
      return;
    }

    ChatResponse resp = ctx.chatResponse();
    Map<String, Object> inputs = Map.of("messages", renderMessages(ctx.chatRequest().messages()));

    // Build defensively: tokenUsage() can be null on pure tool-call responses, aiMessage()
    // text() can be null when only a tool call is returned, and any token count can come back
    // as a null Integer. Map.of rejects all of these.
    Map<String, Object> outputs = new java.util.HashMap<>();
    String text = resp.aiMessage() == null ? null : resp.aiMessage().text();
    outputs.put("output", text == null ? "" : text);
    outputs.put("model", String.valueOf(resp.metadata().modelName()));
    outputs.put("finish_reason", String.valueOf(resp.metadata().finishReason()));
    if (resp.tokenUsage() != null) {
      Map<String, Object> tokens = new java.util.HashMap<>();
      tokens.put("input", nullToZero(resp.tokenUsage().inputTokenCount()));
      tokens.put("output", nullToZero(resp.tokenUsage().outputTokenCount()));
      tokens.put("total", nullToZero(resp.tokenUsage().totalTokenCount()));
      outputs.put("tokens", tokens);
    }

    langSmith.postCompletedRun(
        runId,
        String.valueOf(resp.metadata().modelName()),
        "llm",
        start,
        Instant.now(),
        inputs,
        outputs,
        null,
        traceId,
        parent);
  }

  @Override
  public void onError(ChatModelErrorContext ctx) {
    UUID runId = (UUID) ctx.attributes().get(RUN_ID);
    Instant start = (Instant) ctx.attributes().get(START_TIME);
    UUID parent = (UUID) ctx.attributes().get(PARENT);
    UUID traceId = (UUID) ctx.attributes().get(TRACE_ID);
    if (runId == null || start == null) {
      return;
    }
    langSmith.postCompletedRun(
        runId,
        "chat-error",
        "llm",
        start,
        Instant.now(),
        Map.of("messages", renderMessages(ctx.chatRequest().messages())),
        null,
        ctx.error().getMessage(),
        traceId,
        parent);
  }

  private static Integer nullToZero(Integer i) {
    return i == null ? 0 : i;
  }

  /** Render chat messages as a list of simple maps for LangSmith inputs. */
  private static List<Map<String, String>> renderMessages(List<ChatMessage> messages) {
    return messages.stream()
        .map(
            m ->
                Map.of(
                    "type", m.type().name(),
                    "content", String.valueOf(m)))
        .toList();
  }
}
