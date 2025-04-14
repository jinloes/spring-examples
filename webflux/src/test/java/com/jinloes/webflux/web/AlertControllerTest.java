package com.jinloes.webflux.web;


import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.webflux.config.SecurityConfig;
import com.jinloes.webflux.model.Alert;
import com.jinloes.webflux.model.Severity;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@WebFluxTest(AlertController.class)
@ContextConfiguration(classes = AlertControllerTest.TestConfig.class)
class AlertControllerTest {
  @Autowired
  private KeyPair testKeyPair;

  @Autowired
  private WebTestClient client;

  private String token;


  @TestConfiguration
  @Import(SecurityConfig.class)
  @ComponentScan("com.jinloes.webflux")
  static class TestConfig {
  }

  @BeforeEach
  void setUp() {
    token = Jwts.builder()
        .claim("tenantId", "tenant1")
        .signWith(testKeyPair.getPrivate(), Jwts.SIG.RS256)
        .compact();
  }

  @Test
  void create() {
    client.post()
        .uri("/alerts")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .body(BodyInserters.fromValue(new Alert(null, "Failed", Severity.HIGH)))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(Alert.class)
        .consumeWith(response -> {
          Alert alert = response.getResponseBody();
          assertThat(alert)
              .usingRecursiveComparison()
              .ignoringFields("id")
              .isEqualTo(new Alert(null, "Failed", Severity.HIGH));
          assertThat(alert.id()).isNotNull();
        });
  }

  @Test
  void createNoToken() {
    client.post()
        .uri("/alerts")
        .body(BodyInserters.fromValue(new Alert(null, "Failed", Severity.HIGH)))
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}