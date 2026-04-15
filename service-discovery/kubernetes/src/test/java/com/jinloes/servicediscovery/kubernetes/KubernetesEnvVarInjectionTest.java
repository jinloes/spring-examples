package com.jinloes.servicediscovery.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

/**
 * Tests the env-var injection path of ServiceDiscoveryController using Spring properties to
 * simulate what the kubelet would inject into a real pod.
 *
 * <p>When Kubernetes schedules a pod it injects environment variables for every ClusterIP Service
 * in the same namespace:
 *
 * <pre>
 *   &lt;UPPER_SNAKE_SERVICE_NAME&gt;_SERVICE_HOST=&lt;clusterIP&gt;
 *   &lt;UPPER_SNAKE_SERVICE_NAME&gt;_SERVICE_PORT=&lt;port&gt;
 * </pre>
 *
 * <p>Spring's {@code Environment} abstraction looks up both OS env vars and Spring properties, so
 * setting them via {@code properties} here exactly matches what the controller reads at runtime
 * inside a cluster.
 *
 * <p>NOTE: The {@code /api/call/{serviceName}} endpoint resolves
 * {@code <name>.default.svc.cluster.local} via CoreDNS. CoreDNS is only reachable from inside the
 * cluster, so that endpoint can only be fully tested by deploying the app as a pod in K3s/K8s.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      // Simulates kubelet injecting these for a ClusterIP Service named "payment-service"
      "PAYMENT_SERVICE_SERVICE_HOST=10.96.42.100",
      "PAYMENT_SERVICE_SERVICE_PORT=8080",
    })
class KubernetesEnvVarInjectionTest {

  @LocalServerPort int port;

  RestClient restClient;

  @BeforeEach
  void setUp() {
    restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void envEndpointReturnsInjectedHostForHyphenatedServiceName() {
    // "payment-service" → key prefix "PAYMENT_SERVICE" (upper-cased, hyphens → underscores)
    String response =
        restClient.get().uri("/api/env/payment-service").retrieve().body(String.class);
    assertThat(response).contains("10.96.42.100");
    assertThat(response).contains("8080");
  }

  @Test
  void envEndpointReturnsNotInjectedWhenServiceNotPresent() {
    // No env var is set for "unknown-service", so the controller returns the fallback
    String response =
        restClient.get().uri("/api/env/unknown-service").retrieve().body(String.class);
    assertThat(response).contains("not-injected");
  }
}