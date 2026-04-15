package com.jinloes.messaging.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisWebSocketApplicationTest {

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @LocalServerPort int port;

  RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void contextLoads() {}

  @Test
  void broadcastReturnsAccepted() {
    ResponseEntity<Void> response =
        restClient
            .post()
            .uri("/api/messages")
            .contentType(MediaType.TEXT_PLAIN)
            .body("hello everyone")
            .retrieve()
            .toBodilessEntity();

    assertThat(response.getStatusCode().value()).isEqualTo(202);
  }

  @Test
  void sendToUserReturnsAccepted() {
    // The message is published to Redis and every instance checks its local SimpUserRegistry.
    // "alice" is not connected in this test, so no delivery occurs — but the publish itself
    // succeeds and the endpoint returns 202.
    ResponseEntity<Void> response =
        restClient
            .post()
            .uri("/api/messages/alice")
            .contentType(MediaType.TEXT_PLAIN)
            .body("hello alice")
            .retrieve()
            .toBodilessEntity();

    assertThat(response.getStatusCode().value()).isEqualTo(202);
  }
}
