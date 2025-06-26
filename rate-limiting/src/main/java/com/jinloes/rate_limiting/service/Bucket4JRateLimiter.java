package com.jinloes.rate_limiting.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class Bucket4JRateLimiter {

  private final ProxyManager<String> proxyManager;

  /**
   * Check if request is allowed based on rate limit
   *
   * @param key          unique identifier (e.g., user ID, IP address)
   * @param tokens       number of tokens to consume
   * @param capacity     maximum number of tokens in bucket
   * @param refillTokens number of tokens to refill
   * @param refillPeriod period for refill
   * @return RateLimitResult containing whether request is allowed and metadata
   */
  public RateLimitResult isRequestAllowed(String key, long tokens, long capacity, long refillTokens,
      Duration refillPeriod) {

    Supplier<BucketConfiguration> configSupplier = () -> {
      Bandwidth limit = Bandwidth.builder()
          .capacity(capacity)
          .refillIntervally(refillTokens, refillPeriod)
          .build();

      return BucketConfiguration.builder()
          .addLimit(limit)
          .build();
    };

    Bucket bucket = proxyManager.builder()
        .build(key, configSupplier);

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(tokens);

    if (probe.isConsumed()) {
      log.debug("Request allowed for key: {}, remaining tokens: {}", key, probe.getRemainingTokens());
      return RateLimitResult.allowed(probe.getRemainingTokens());
    } else {
      log.debug("Request denied for key: {}, retry after: {} seconds",
          key, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
      return RateLimitResult.denied(
          probe.getRemainingTokens(),
          Duration.ofNanos(probe.getNanosToWaitForRefill())
      );
    }
  }

  /**
   * Convenience method for common rate limiting scenarios
   */
  public RateLimitResult isRequestAllowed(String key) {
    // Default: 100 requests per minute
    return isRequestAllowed(key, 1, 100, 100, Duration.ofMinutes(1));
  }

  /**
   * API rate limiting: 1000 requests per hour
   */
  public RateLimitResult isApiRequestAllowed(String apiKey) {
    return isRequestAllowed("api:" + apiKey, 1, 1000, 1000, Duration.ofHours(1));
  }

  /**
   * User action rate limiting: 10 requests per minute
   */
  public RateLimitResult isUserActionAllowed(String userId) {
    return isRequestAllowed("user:" + userId, 1, 10, 10, Duration.ofMinutes(1));
  }
}
