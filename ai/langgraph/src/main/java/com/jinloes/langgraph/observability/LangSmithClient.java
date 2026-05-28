package com.jinloes.langgraph.observability;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client for LangSmith's run ingestion API. Each completed span is posted as a single Run
 * object with start_time, end_time, inputs, outputs, parent_run_id, and trace_id already set.
 *
 * <p>If LangSmith is disabled or no API key is configured, every method becomes a no-op — the graph
 * still runs, you just don't see traces in LangSmith.
 *
 * <p>API ref:
 * https://docs.smith.langchain.com/observability/how_to_guides/log_traces_with_api_directly
 */
@Slf4j
@Component
@Configuration
@EnableConfigurationProperties(LangSmithProperties.class)
public class LangSmithClient {

  private final LangSmithProperties props;
  private final RestClient http;

  public LangSmithClient(LangSmithProperties props) {
    this.props = props;
    // RestClient.builder() is the static factory; we don't inject RestClient.Builder because
    // the Gemini starter (unlike the OpenAI starter) doesn't bring spring-web's REST client
    // auto-configuration transitively. The static builder works identically.
    this.http =
        props.active()
            ? RestClient.builder()
                .baseUrl(props.endpoint())
                .defaultHeader("x-api-key", props.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build()
            : null;
    if (props.active()) {
      log.info(
          "LangSmith tracing enabled: endpoint=%s project=%s"
              .formatted(props.endpoint(), props.project()));
    } else {
      log.info("LangSmith tracing disabled (no API key or explicitly disabled)");
    }
  }

  /** Generate a run id ahead of starting the run, so it can be used as parent for child runs. */
  public UUID newRunId() {
    return UUID.randomUUID();
  }

  /**
   * Post a completed run in one call. We post on completion (not on start) for simplicity —
   * LangSmith UI shows runs only once they have both timestamps anyway.
   */
  public void postCompletedRun(
      UUID runId,
      String name,
      String runType,
      Instant startTime,
      Instant endTime,
      Map<String, Object> inputs,
      Map<String, Object> outputs,
      String error,
      UUID traceId,
      UUID parentRunId) {

    if (http == null) {
      return;
    }

    Map<String, Object> body = new HashMap<>();
    body.put("id", runId.toString());
    body.put("name", name);
    body.put("run_type", runType);
    body.put("start_time", startTime.toString());
    body.put("end_time", endTime.toString());
    body.put("inputs", inputs == null ? Map.of() : inputs);
    if (outputs != null) {
      body.put("outputs", outputs);
    }
    if (error != null) {
      body.put("error", error);
    }
    body.put("trace_id", (traceId == null ? runId : traceId).toString());
    if (parentRunId != null) {
      body.put("parent_run_id", parentRunId.toString());
    }
    body.put("session_name", props.project());

    try {
      http.post().uri("/runs").body(body).retrieve().toBodilessEntity();
    } catch (Exception e) {
      // Observability must never break the application: log and move on.
      log.warn(
          "LangSmith run post failed (name=%s runId=%s): %s"
              .formatted(name, runId, e.getMessage()));
    }
  }
}
