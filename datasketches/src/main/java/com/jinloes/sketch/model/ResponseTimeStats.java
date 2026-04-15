package com.jinloes.sketch.model;

/** Response time percentiles derived from a Quantiles sketch, in milliseconds. */
public record ResponseTimeStats(double p50, double p95, double p99, long count) {}
