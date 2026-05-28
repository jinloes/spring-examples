package com.jinloes.langgraph.observability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Per-request trace context. The triage controller pushes the root run id when a request begins;
 * graph node hooks push a node-level run id around each node execution; the chat-model listener
 * reads the top of the stack to attribute its LLM run to the right parent.
 *
 * <p>Because we use synchronous {@code node_async(syncNode)} wrappers — which execute on the
 * calling thread — a ThreadLocal stack is sufficient. If you switch to genuinely async nodes
 * (CompletableFuture chains across pools), replace this with {@code RunnableConfig} metadata.
 */
@Component
public class LangSmithTraceContext {

  private static final ThreadLocal<UUID> TRACE_ID = new ThreadLocal<>();
  private static final ThreadLocal<Deque<UUID>> PARENT_STACK =
      ThreadLocal.withInitial(ArrayDeque::new);

  public void beginTrace(UUID traceId, UUID rootRunId) {
    TRACE_ID.set(traceId);
    PARENT_STACK.get().push(rootRunId);
  }

  public void endTrace() {
    TRACE_ID.remove();
    PARENT_STACK.remove();
  }

  public void pushParent(UUID runId) {
    PARENT_STACK.get().push(runId);
  }

  public void popParent() {
    Deque<UUID> stack = PARENT_STACK.get();
    if (!stack.isEmpty()) {
      stack.pop();
    }
  }

  /** Returns the current parent run id, or null if no trace is active. */
  public UUID currentParent() {
    Deque<UUID> stack = PARENT_STACK.get();
    return stack.isEmpty() ? null : stack.peek();
  }

  /** Returns the active trace id, or null if no trace is active. */
  public UUID currentTraceId() {
    return TRACE_ID.get();
  }
}
