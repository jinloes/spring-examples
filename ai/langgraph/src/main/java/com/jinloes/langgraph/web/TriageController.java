package com.jinloes.langgraph.web;

import com.jinloes.langgraph.graph.TriageState;
import com.jinloes.langgraph.observability.LangSmithClient;
import com.jinloes.langgraph.observability.LangSmithTraceContext;
import com.jinloes.langgraph.observability.PendingActionsRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/triage")
@RequiredArgsConstructor
public class TriageController {

  public record TriageRequest(String inquiry, String sessionId) {}

  public record TriageResponse(
      String sessionId, String traceId, String intent, String response, List<UiAction> actions) {}

  private final CompiledGraph<TriageState> triageGraph;
  private final LangSmithClient langSmith;
  private final LangSmithTraceContext traceContext;
  private final PendingActionsRegistry pendingActions;

  @PostMapping
  public ResponseEntity<TriageResponse> triage(@RequestBody TriageRequest body) {

    // sessionId is the ChatMemory key — same id across turns = same conversation. Generate one
    // on the first turn if the client didn't supply one.
    String sessionId =
        (body.sessionId() == null || body.sessionId().isBlank())
            ? UUID.randomUUID().toString()
            : body.sessionId();

    // Begin a trace: one root run that becomes the parent of every node + LLM call below.
    UUID traceId = UUID.randomUUID();
    UUID rootRunId = langSmith.newRunId();
    Instant start = Instant.now();
    traceContext.beginTrace(traceId, rootRunId);

    try {
      log.info(
          "Triage start sessionId=%s traceId=%s inquiry='%s'"
              .formatted(sessionId, traceId, body.inquiry()));

      TriageState finalState =
          triageGraph
              .invoke(
                  Map.of(
                      TriageState.INQUIRY, body.inquiry(),
                      TriageState.SESSION_ID, sessionId,
                      TriageState.TRACE_ID, traceId.toString()))
              .orElseThrow(() -> new IllegalStateException("Graph produced no final state"));

      String intent = finalState.intent().orElse("unknown");
      String response = finalState.response().orElse("(no response produced)");

      // Post the root span that contains all the child node/LLM runs.
      langSmith.postCompletedRun(
          rootRunId,
          "support_triage",
          "chain",
          start,
          Instant.now(),
          Map.of("inquiry", body.inquiry(), "sessionId", sessionId),
          Map.of("intent", intent, "response", response),
          null,
          traceId,
          null);

      // Drain any UI actions queued by tools during this turn (e.g. an order picker).
      List<UiAction> actions = pendingActions.drain(sessionId);

      return ResponseEntity.ok(
          new TriageResponse(sessionId, traceId.toString(), intent, response, actions));

    } catch (Exception e) {
      langSmith.postCompletedRun(
          rootRunId,
          "support_triage",
          "chain",
          start,
          Instant.now(),
          Map.of("inquiry", body.inquiry(), "sessionId", sessionId),
          null,
          e.toString(),
          traceId,
          null);
      throw e;
    } finally {
      traceContext.endTrace();
    }
  }
}
