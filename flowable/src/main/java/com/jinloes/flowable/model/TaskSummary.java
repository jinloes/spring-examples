package com.jinloes.flowable.model;

import java.util.Map;

/** Summary of a pending user task waiting for human action. */
public record TaskSummary(
    String taskId, String name, String processInstanceId, Map<String, Object> variables) {}
