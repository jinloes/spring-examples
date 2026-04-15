package com.jinloes.loom_web.model;

import java.util.List;

public record FanoutResponse(
    int tasks, long taskDelayMs, long totalElapsedMs, List<ServiceResult> results) {}
