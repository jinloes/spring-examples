package com.jinloes.rate_limiting.service;

import java.time.Duration;

public record RateLimitResult(boolean allowed, long remainingTokens, Duration retryAfter) {

  public static RateLimitResult allowed(long remainingTokens) {
    return new RateLimitResult(true, remainingTokens, Duration.ZERO);
  }

  public static RateLimitResult denied(long remainingTokens, Duration retryAfter) {
    return new RateLimitResult(false, remainingTokens, retryAfter);
  }
}
