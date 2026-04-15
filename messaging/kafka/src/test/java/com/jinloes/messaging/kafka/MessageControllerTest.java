package com.jinloes.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
    partitions = 1,
    topics = "messages",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class MessageControllerTest {

  @LocalServerPort int port;

  RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void publishReturnsAccepted() {
    ResponseEntity<Void> response =
        restClient
            .post()
            .uri("/api/topics/messages")
            .contentType(MediaType.TEXT_PLAIN)
            .body("hello world")
            .retrieve()
            .toBodilessEntity();

    assertThat(response.getStatusCode().value()).isEqualTo(202);
  }
}
