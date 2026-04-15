package com.jinloes.sketch.model;

/**
 * A single analytics event. All fields are optional — null values are silently skipped so partial
 * events (e.g. a page view without a known user) are still useful.
 */
public record Event(String userId, String pageUrl, Double responseTimeMs) {}
