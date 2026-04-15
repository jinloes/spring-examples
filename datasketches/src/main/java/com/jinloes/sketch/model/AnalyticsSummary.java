package com.jinloes.sketch.model;

import java.util.List;

/** Full analytics snapshot across all four sketch types. */
public record AnalyticsSummary(
    UniqueEstimate uniqueVisitors,
    UniqueEstimate uniquePages,
    ResponseTimeStats responseTimes,
    List<PageFrequency> popularPages) {}
