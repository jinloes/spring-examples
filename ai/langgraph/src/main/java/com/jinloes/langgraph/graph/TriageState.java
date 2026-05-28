package com.jinloes.langgraph.graph;

import java.util.Map;
import java.util.Optional;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

/**
 * Shared state passed between graph nodes. All keys use the default overwrite reducer — each node
 * returns a Map that replaces the listed keys.
 */
public class TriageState extends AgentState {

  public static final String INQUIRY = "inquiry";
  public static final String INTENT = "intent";
  public static final String RESPONSE = "response";
  public static final String TRACE_ID = "traceId";
  public static final String SESSION_ID = "sessionId";

  public static final Map<String, Channel<?>> SCHEMA = Map.of();

  public TriageState(Map<String, Object> initData) {
    super(initData);
  }

  public String inquiry() {
    return this.<String>value(INQUIRY).orElseThrow();
  }

  public Optional<String> intent() {
    return value(INTENT);
  }

  public Optional<String> response() {
    return value(RESPONSE);
  }

  public Optional<String> traceId() {
    return value(TRACE_ID);
  }

  public String sessionId() {
    return this.<String>value(SESSION_ID).orElseThrow();
  }
}
