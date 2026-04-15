package com.jinloes.sketch.model;

/**
 * Probabilistic cardinality estimate with 95% confidence bounds.
 *
 * <p>The true count falls between {@code lowerBound} and {@code upperBound} with ~95% probability
 * (2 standard deviations). For large N the relative error is typically &lt;2%.
 */
public record UniqueEstimate(long estimate, long lowerBound, long upperBound) {}
