package com.jinloes.servicediscovery.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KubernetesDiscoveryApplicationTest {

  @LocalServerPort int port;

  RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void contextLoads() {}

  @Test
  void helloEndpointReturnsServiceName() {
    String response = restClient.get().uri("/api/hello").retrieve().body(String.class);
    assertThat(response).contains("kubernetes-discovery");
  }

  @Test
  void envEndpointReturnsNotInjectedOutsideCluster() {
    String response = restClient.get().uri("/api/env/my-service").retrieve().body(String.class);
    assertThat(response).contains("not-injected");
  }
}
