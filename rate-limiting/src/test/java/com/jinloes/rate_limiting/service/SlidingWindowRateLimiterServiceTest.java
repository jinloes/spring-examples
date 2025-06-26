package com.jinloes.rate_limiting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class SlidingWindowRateLimiterServiceTest {

  @Container
  private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired
  private SlidingWindowRateLimiterService rateLimiterService;

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @BeforeEach
  void setUp() {
    // Clean up Redis before each test
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Test
  void shouldAllowRequestsWithinLimit() {
    // Given
    String clientId = "client1";
    int limit = 5;
    Duration window = Duration.ofMinutes(1);

    // When & Then
    for (int i = 0; i < limit; i++) {
      boolean allowed = rateLimiterService.isAllowed(clientId, limit, window);
      assertThat(allowed).isTrue();
    }
  }

  @Test
  void shouldDenyRequestsExceedingLimit() {
    // Given
    String clientId = "client2";
    int limit = 3;
    Duration window = Duration.ofMinutes(1);

    // When - consume all allowed requests
    for (int i = 0; i < limit; i++) {
      rateLimiterService.isAllowed(clientId, limit, window);
    }

    // Then - next request should be denied
    boolean allowed = rateLimiterService.isAllowed(clientId, limit, window);
    assertThat(allowed).isFalse();
  }

  @Test
  void shouldAllowRequestsAfterWindowSlides() throws InterruptedException {
    // Given
    String clientId = "client3";
    int limit = 2;
    Duration window = Duration.ofSeconds(2);

    // When - consume all allowed requests
    for (int i = 0; i < limit; i++) {
      rateLimiterService.isAllowed(clientId, limit, window);
    }

    // Should be denied immediately
    assertThat(rateLimiterService.isAllowed(clientId, limit, window)).isFalse();

    // Wait for window to slide
    Thread.sleep(2100);

    // Then - should allow requests again
    boolean allowed = rateLimiterService.isAllowed(clientId, limit, window);
    assertThat(allowed).isTrue();
  }

  @Test
  void shouldHandleDifferentClients() {
    // Given
    String clientId1 = "client4";
    String clientId2 = "client5";
    int limit = 2;
    Duration window = Duration.ofMinutes(1);

    // When - client1 exceeds limit
    for (int i = 0; i < limit; i++) {
      rateLimiterService.isAllowed(clientId1, limit, window);
    }

    // Then - client1 should be denied but client2 should be allowed
    assertThat(rateLimiterService.isAllowed(clientId1, limit, window)).isFalse();
    assertThat(rateLimiterService.isAllowed(clientId2, limit, window)).isTrue();
  }

  @Test
  void shouldThrowExceptionForNullClientId() {
    // Given
    String clientId = null;
    int limit = 5;
    Duration window = Duration.ofMinutes(1);

    // When & Then
    assertThatThrownBy(() -> rateLimiterService.isAllowed(clientId, limit, window))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Client ID cannot be null or empty");
  }

  @Test
  void shouldThrowExceptionForInvalidLimit() {
    // Given
    String clientId = "client6";
    int limit = 0;
    Duration window = Duration.ofMinutes(1);

    // When & Then
    assertThatThrownBy(() -> rateLimiterService.isAllowed(clientId, limit, window))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Limit must be greater than 0");
  }

  @Test
  void shouldThrowExceptionForNullWindow() {
    // Given
    String clientId = "client7";
    int limit = 5;
    Duration window = null;

    // When & Then
    assertThatThrownBy(() -> rateLimiterService.isAllowed(clientId, limit, window))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Window duration cannot be null");
  }

  @Test
  void shouldHandleVerySmallWindow() {
    // Given
    String clientId = "small-window-client";
    int limit = 2;
    Duration window = Duration.ofMillis(500);

    // When & Then
    for (int i = 0; i < limit; i++) {
      boolean allowed = rateLimiterService.isAllowed(clientId, limit, window);
      assertThat(allowed).isTrue();
    }

    // Should be denied when limit is exceeded
    assertThat(rateLimiterService.isAllowed(clientId, limit, window)).isFalse();
  }
}
