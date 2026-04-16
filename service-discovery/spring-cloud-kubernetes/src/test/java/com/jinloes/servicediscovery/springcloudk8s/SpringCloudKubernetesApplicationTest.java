package com.jinloes.servicediscovery.springcloudk8s;

import static org.assertj.core.api.Assertions.assertThat;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for Spring Cloud Kubernetes discovery against a real K3s cluster.
 *
 * <p>Spring Cloud Kubernetes backs {@link
 * org.springframework.cloud.client.discovery.DiscoveryClient} with the Kubernetes API client. By
 * overriding the {@link ApiClient} bean with one pointing at K3s, the discovery endpoints are
 * tested against a real (containerized) cluster — no mocking required.
 *
 * <p>Extension ordering: Testcontainers starts K3S before Spring loads the application context
 * (Testcontainers extension runs its {@code beforeAll} before Spring's), so {@link K3sClientConfig}
 * can safely read {@code K3S.getKubeConfigYaml()} during bean creation.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // Disable ConfigMap/Secret config loading — this module only demos service discovery
    properties = "spring.cloud.kubernetes.config.enabled=false")
@Import(SpringCloudKubernetesApplicationTest.K3sClientConfig.class)
class SpringCloudKubernetesApplicationTest {

  @Container
  static final K3sContainer K3S =
      new K3sContainer(DockerImageName.parse("rancher/k3s:v1.30.0-k3s1"));

  @LocalServerPort int port;

  RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  /**
   * Creates a Service in K3s before tests run so discovery returns more than the cluster default.
   */
  @BeforeAll
  static void createTestService() throws IOException, InterruptedException {
    K3S.execInContainer(
        "k3s", "kubectl", "create", "service", "clusterip", "test-service", "--tcp=8080:8080");
  }

  @Test
  void servicesEndpointListsDefaultKubernetesService() {
    // K3s bootstraps a "kubernetes" Service in the default namespace at cluster startup
    ResponseEntity<List<String>> response =
        restClient
            .get()
            .uri("/api/services")
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() {});
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("kubernetes");
  }

  @Test
  void servicesEndpointListsServiceCreatedInK3s() {
    List<String> services =
        restClient
            .get()
            .uri("/api/services")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    assertThat(services).contains("test-service");
  }

  @Test
  void instancesEndpointReturnsDetailsForKubernetesService() {
    List<?> instances =
        restClient
            .get()
            .uri("/api/instances/kubernetes")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    assertThat(instances).isNotEmpty();
  }

  /**
   * Overrides the auto-configured {@link ApiClient} with one pointing at the K3s test cluster.
   *
   * <p>Spring Cloud Kubernetes constructs {@code CoreV1Api} (and therefore {@code DiscoveryClient})
   * by injecting the {@link ApiClient} bean, so this single override redirects all cluster
   * communication to K3s.
   */
  @TestConfiguration(proxyBeanMethods = false)
  static class K3sClientConfig {

    @Bean
    @Primary
    ApiClient k3sApiClient() throws IOException {
      ApiClient client =
          Config.fromConfig(
              new ByteArrayInputStream(K3S.getKubeConfigYaml().getBytes(StandardCharsets.UTF_8)));
      // Also set as the global default so any code using Configuration.getDefaultApiClient()
      // (outside of the Spring bean graph) also points at K3s
      Configuration.setDefaultApiClient(client);
      return client;
    }
  }
}
