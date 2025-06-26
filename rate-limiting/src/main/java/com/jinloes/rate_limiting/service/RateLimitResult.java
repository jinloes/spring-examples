package com.jinloes.rate_limiting.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@Data
@AllArgsConstructor
public class RateLimitResult {
  private final boolean allowed;
  private final long remainingTokens;
  private final Duration retryAfter;

  public static RateLimitResult allowed(long remainingTokens) {
    return new RateLimitResult(true, remainingTokens, Duration.ZERO);
  }

  public static RateLimitResult denied(long remainingTokens, Duration retryAfter) {
    return new RateLimitResult(false, remainingTokens, retryAfter);
  }
}