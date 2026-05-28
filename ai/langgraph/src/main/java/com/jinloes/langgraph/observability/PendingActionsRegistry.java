package com.jinloes.langgraph.observability;

import com.jinloes.langgraph.web.UiAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Per-session buffer of UI actions queued by tools during a {@code /triage} call. The controller
 * drains the buffer after the graph runs and attaches the actions to the HTTP response so the chat
 * UI can render them (e.g. as clickable order buttons).
 *
 * <p>This is the side channel that lets a {@code @Tool} method "show buttons" without forcing the
 * LLM to emit structured markers in its text response.
 */
@Component
public class PendingActionsRegistry {

  private final Map<String, List<UiAction>> bySession = new ConcurrentHashMap<>();

  public void add(String sessionId, UiAction action) {
    if (sessionId == null) {
      return;
    }
    bySession
        .computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(action);
  }

  /** Return and clear actions for this session. Called once per /triage response. */
  public List<UiAction> drain(String sessionId) {
    List<UiAction> taken = bySession.remove(sessionId);
    return taken == null ? List.of() : List.copyOf(taken);
  }
}
