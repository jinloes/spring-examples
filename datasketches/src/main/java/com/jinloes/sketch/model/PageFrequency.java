package com.jinloes.sketch.model;

/** A page URL and its estimated visit count from the Frequency sketch. */
public record PageFrequency(String page, long estimate) {}
