package com.jinloes.servicediscovery.consul;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Consul registration and config import are disabled so the context loads without a running agent
@SpringBootTest(
    properties = {
      "spring.cloud.consul.enabled=false",
      "spring.cloud.consul.config.enabled=false",
      "spring.cloud.consul.discovery.enabled=false",
      "spring.cloud.consul.discovery.register=false"
    })
class ConsulDiscoveryApplicationTest {

  @Test
  void contextLoads() {}
}
