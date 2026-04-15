package com.jinloes.servicediscovery.kubernetes.web;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ServiceDiscoveryController {

  private final RestTemplate restTemplate;
  private final Environment environment;

  @GetMapping("/hello")
  public String hello() {
    return "Hello from %s"
        .formatted(environment.getProperty("spring.application.name", "unknown"));
  }

  // Demonstrates calling another service using Kubernetes DNS.
  // K8s creates a DNS record for every Service: <service-name>.<namespace>.svc.cluster.local
  // This only works when the app is running inside a K8s cluster.
  @GetMapping("/call/{serviceName}")
  public String callService(@PathVariable String serviceName) {
    String url = "http://%s.default.svc.cluster.local/api/hello".formatted(serviceName);
    log.info("Calling service via K8s DNS: {}", url);
    return restTemplate.getForObject(url, String.class);
  }

  // K8s also injects environment variables for every Service in the same namespace:
  // <SERVICE_NAME>_SERVICE_HOST and <SERVICE_NAME>_SERVICE_PORT
  @GetMapping("/env/{serviceName}")
  public Map<String, String> getServiceEnv(@PathVariable String serviceName) {
    String key = serviceName.toUpperCase().replace("-", "_");
    Map<String, String> vars = new LinkedHashMap<>();
    vars.put("SERVICE_HOST", environment.getProperty(key + "_SERVICE_HOST", "not-injected"));
    vars.put("SERVICE_PORT", environment.getProperty(key + "_SERVICE_PORT", "not-injected"));
    return vars;
  }
}
