package com.jinloes.flowable.model;

import java.util.Date;
import java.util.Map;

/** Current state of a loan approval process instance. */
public record ProcessStatus(
    String processInstanceId, String status, Map<String, Object> variables, Date endTime) {}
