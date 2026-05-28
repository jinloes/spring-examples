package com.jinloes.langgraph.observability;

import org.springframework.stereotype.Component;

/**
 * ThreadLocal sessionId so a {@code @Tool} method invoked deep inside an {@code AiServices} proxy
 * can identify which conversation it's running for, without altering the tool method's parameter
 * list (which would change the prompt the LLM sees).
 *
 * <p>Safe because the refund agent's {@code AiServices} proxy executes synchronously on the calling
 * thread when invoked from {@code node_async(syncNode)} — same reason {@link LangSmithTraceContext}
 * uses ThreadLocal.
 */
@Component
public class SessionContext {

  private static final ThreadLocal<String> SID = new ThreadLocal<>();

  public void set(String sessionId) {
    SID.set(sessionId);
  }

  public void clear() {
    SID.remove();
  }

  public String get() {
    return SID.get();
  }
}
