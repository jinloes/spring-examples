package com.jinloes.rate_limiting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureCache
@Testcontainers
class Bucket4JRateLimiterTest {

  @Container
  private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired
  private Bucket4JRateLimiter rateLimiter;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @BeforeEach
  void setUp() {
    // Clean up Redis before each test
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void shouldAllowRequestWhenUnderLimit() {
    // Given
    String key = "test-user";

    // When
    RateLimitResult result = rateLimiter.isRequestAllowed(
        key, 1, 5, 5, Duration.ofMinutes(1)
    );

    // Then
    assertThat(result.isAllowed())
        .isTrue();
    assertThat(result.getRemainingTokens())
        .isEqualTo(4);
    assertThat(result.getRetryAfter())
        .isEqualTo(Duration.ZERO);
  }

  @Test
  void shouldDenyRequestWhenOverLimit() {
    // Given
    String key = "test-user-limit";
    int capacity = 3;

    // When - consume all tokens
    for (int i = 0; i < capacity; i++) {
      RateLimitResult result = rateLimiter.isRequestAllowed(
          key, 1, capacity, capacity, Duration.ofMinutes(1)
      );
      assertThat(result.isAllowed()).isTrue();
    }

    // When - exceed limit
    RateLimitResult result = rateLimiter.isRequestAllowed(
        key, 1, capacity, capacity, Duration.ofMinutes(1)
    );

    // Then
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getRemainingTokens()).isZero();
    assertThat(result.getRetryAfter()).isNotNull();
    assertThat(result.getRetryAfter().toSeconds()).isGreaterThan(0);
  }

  @Test
  void shouldConsumeMultipleTokens() {
    // Given
    String key = "multi-token-user";
    int capacity = 10;
    int tokensToConsume = 5;

    // When
    RateLimitResult result = rateLimiter.isRequestAllowed(
        key, tokensToConsume, capacity, capacity, Duration.ofMinutes(1)
    );

    // Then
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRemainingTokens()).isEqualTo(capacity - tokensToConsume);
  }

  @Test
  void shouldDenyWhenRequestingMoreTokensThanCapacity() {
    // Given
    String key = "over-capacity-user";
    int capacity = 5;
    int tokensToConsume = 10;

    // When
    RateLimitResult result = rateLimiter.isRequestAllowed(
        key, tokensToConsume, capacity, capacity, Duration.ofMinutes(1)
    );

    // Then
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getRetryAfter()).isNotNull();
  }

  @Test
  void shouldRefillTokensOverTime() throws InterruptedException {
    // Given
    String key = "refill-user";
    int capacity = 2;
    Duration refillPeriod = Duration.ofMillis(500);

    // When - consume all tokens
    RateLimitResult result1 = rateLimiter.isRequestAllowed(
        key, 1, capacity, 1, refillPeriod
    );
    RateLimitResult result2 = rateLimiter.isRequestAllowed(
        key, 1, capacity, 1, refillPeriod
    );

    // Then - should be at limit
    assertThat(result1.isAllowed()).isTrue();
    assertThat(result2.isAllowed()).isTrue();

    // When - try to consume another token immediately
    RateLimitResult result3 = rateLimiter.isRequestAllowed(
        key, 1, capacity, 1, refillPeriod
    );

    // Then - should be denied
    assertThat(result3.isAllowed()).isFalse();

    // When - wait for refill and try again
    Thread.sleep(600); // Wait longer than refill period
    RateLimitResult result4 = rateLimiter.isRequestAllowed(
        key, 1, capacity, 1, refillPeriod
    );

    // Then - should be allowed after refill
    assertThat(result4.isAllowed()).isTrue();
  }

  @Test
  void shouldIsolateBucketsByKey() {
    // Given
    String key1 = "user-1";
    String key2 = "user-2";
    int capacity = 2;

    // When - consume all tokens for user-1
    rateLimiter.isRequestAllowed(key1, 1, capacity, capacity, Duration.ofMinutes(1));
    rateLimiter.isRequestAllowed(key1, 1, capacity, capacity, Duration.ofMinutes(1));

    RateLimitResult result1 = rateLimiter.isRequestAllowed(
        key1, 1, capacity, capacity, Duration.ofMinutes(1)
    );

    // When - try with user-2
    RateLimitResult result2 = rateLimiter.isRequestAllowed(
        key2, 1, capacity, capacity, Duration.ofMinutes(1)
    );

    // Then
    assertThat(result1.isAllowed()).isFalse(); // user-1 is rate limited
    assertThat(result2.isAllowed()).isTrue();  // user-2 is not affected
  }

  @Test
  void shouldHandleConcurrentRequests() throws InterruptedException {
    // Given
    String key = "concurrent-user";
    int capacity = 10;
    int numberOfThreads = 20;
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

    // When - make concurrent requests
    var results = new RateLimitResult[numberOfThreads];
    for (int i = 0; i < numberOfThreads; i++) {
      final int index = i;
      executor.submit(() -> {
        try {
          results[index] = rateLimiter.isRequestAllowed(
              key, 1, capacity, capacity, Duration.ofMinutes(1)
          );
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // Then - only 'capacity' requests should be allowed
    long allowedCount = IntStream.range(0, numberOfThreads)
        .mapToObj(i -> results[i])
        .filter(RateLimitResult::isAllowed)
        .count();

    assertThat(allowedCount).isEqualTo(capacity);

    long deniedCount = IntStream.range(0, numberOfThreads)
        .mapToObj(i -> results[i])
        .filter(result -> !result.isAllowed())
        .count();

    assertThat(deniedCount).isEqualTo(numberOfThreads - capacity);
  }

  @Test
  void shouldUseDefaultRateLimitForConvenienceMethod() {
    // Given
    String key = "default-user";

    // When - use default rate limit (100 requests per minute)
    RateLimitResult result = rateLimiter.isRequestAllowed(key);

    // Then
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRemainingTokens()).isEqualTo(99); // 100 - 1
  }

  @Test
  void shouldApplyApiRateLimit() {
    // Given
    String apiKey = "test-api-key";

    // When
    RateLimitResult result = rateLimiter.isApiRequestAllowed(apiKey);

    // Then
    assertThat(result.isAllowed()).isTrue();
    assertThat(result.getRemainingTokens()).isEqualTo(999); // 1000 - 1
  }

  @Test
  void shouldApplyUserActionRateLimit() {
    // Given
    String userId = "test-user-123";

    // When - consume all allowed actions (10 per minute)
    for (int i = 0; i < 10; i++) {
      RateLimitResult result = rateLimiter.isUserActionAllowed(userId);
      assertThat(result.isAllowed()).isTrue();
    }

    // When - try one more action
    RateLimitResult result = rateLimiter.isUserActionAllowed(userId);

    // Then
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getRetryAfter()).isNotNull();
  }

  @Test
  void shouldDistinguishBetweenDifferentRateLimitTypes() {
    // Given
    String identifier = "same-identifier";

    // When
    RateLimitResult defaultResult = rateLimiter.isRequestAllowed(identifier);
    RateLimitResult apiResult = rateLimiter.isApiRequestAllowed(identifier);
    RateLimitResult userResult = rateLimiter.isUserActionAllowed(identifier);

    // Then - all should be allowed as they use different keys internally
    assertThat(defaultResult.isAllowed()).isTrue();
    assertThat(apiResult.isAllowed()).isTrue();
    assertThat(userResult.isAllowed()).isTrue();

    // And they should have different remaining token counts
    assertThat(defaultResult.getRemainingTokens()).isEqualTo(99);  // 100 - 1
    assertThat(apiResult.getRemainingTokens()).isEqualTo(999);     // 1000 - 1
    assertThat(userResult.getRemainingTokens()).isEqualTo(9);      // 10 - 1
  }
}