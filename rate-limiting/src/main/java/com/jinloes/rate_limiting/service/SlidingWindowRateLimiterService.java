package com.jinloes.rate_limiting.service;

import io.micrometer.common.util.StringUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlidingWindowRateLimiterService {

  private final RedisTemplate<String, Object> redisTemplate;

  public boolean isAllowed(String key, int limit, Duration windowDuration) {
    validateInputs(key, limit, windowDuration);

    String fullKey = "rate_limit:" + key;
    long windowStart = Instant.now().toEpochMilli() - windowDuration.toMillis();

    List<String> keys = Collections.singletonList(fullKey);
    Long result = redisTemplate.execute(createRateLimitScript(), keys,
        String.valueOf(limit),
        String.valueOf(windowStart),
        String.valueOf(Instant.now().toEpochMilli()),
        String.valueOf(windowDuration.toSeconds() == 0 ? 1 : windowDuration.toSeconds()));

    return result != null && result == 0;
  }

  private RedisScript<Long> createRateLimitScript() {
    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setLocation(new ClassPathResource("rate_limit.lua"));
    redisScript.setResultType(Long.class);
    return redisScript;
  }

  private void validateInputs(String key, int limit, Duration window) {
    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("Client ID cannot be null or empty");
    }

    if (limit <= 0) {
      throw new IllegalArgumentException("Limit must be greater than 0");
    }

    if (Objects.isNull(window)) {
      throw new IllegalArgumentException("Window duration cannot be null");
    }

    if (window.isNegative() || window.isZero()) {
      throw new IllegalArgumentException("Window duration must be positive");
    }
  }
}
