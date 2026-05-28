package com.jinloes.langgraph.observability;

import com.jinloes.langgraph.graph.TriageState;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

/**
 * Attaches before/after node hooks that emit a LangSmith run for each graph node execution. Each
 * node run becomes the parent of any LLM calls made inside the node body.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LangSmithGraphHooks {

  private final LangSmithClient langSmith;
  private final LangSmithTraceContext traceContext;

  // Co-located maps keyed by nodeId; safe because the graph runs one node at a time per trace
  // (langgraph4j is single-threaded within a trace unless parallel branches are configured).
  private final ThreadLocal<Map<String, UUID>> nodeRunIds =
      ThreadLocal.withInitial(java.util.HashMap::new);
  private final ThreadLocal<Map<String, Instant>> nodeStartTimes =
      ThreadLocal.withInitial(java.util.HashMap::new);

  public void attach(StateGraph<TriageState> graph) {
    // Before-call hook: stash bookkeeping, then return the current state data unchanged so
    // the next hook (if any) sees the real state. Returning Map.of() works at the tail of
    // the reduce chain (langgraph4j's final updateState short-circuits on empty), but is
    // unsafe if other hooks are chained — return state.data() to be robust.
    graph.addBeforeCallNodeHook(
        (nodeId, state, config) -> {
          UUID runId = langSmith.newRunId();
          nodeRunIds.get().put(nodeId, runId);
          nodeStartTimes.get().put(nodeId, Instant.now());
          traceContext.pushParent(runId);
          log.debug("→ node=%s runId=%s".formatted(nodeId, runId));
          return CompletableFuture.completedFuture(state.data());
        });

    // After-call hook: CRITICAL — return lastResult unchanged. langgraph4j's reduce uses
    // the hook's return value as the new partial state that gets merged into the graph
    // state. Returning Map.of() here would silently discard the node's partial update,
    // wiping out fields like INTENT and RESPONSE between nodes.
    graph.addAfterCallNodeHook(
        (nodeId, state, config, lastResult) -> {
          UUID runId = nodeRunIds.get().remove(nodeId);
          Instant start = nodeStartTimes.get().remove(nodeId);
          UUID traceId = traceContext.currentTraceId();
          traceContext.popParent();
          UUID parent = traceContext.currentParent();

          if (runId != null && start != null) {
            langSmith.postCompletedRun(
                runId,
                nodeId,
                "chain",
                start,
                Instant.now(),
                Map.of("inquiry", state.inquiry()),
                Map.of(
                    "intent", state.intent().orElse(""),
                    "response", state.response().orElse("")),
                null,
                traceId,
                parent);
          }
          log.debug("← node=%s".formatted(nodeId));
          return CompletableFuture.completedFuture(lastResult);
        });
  }
}
