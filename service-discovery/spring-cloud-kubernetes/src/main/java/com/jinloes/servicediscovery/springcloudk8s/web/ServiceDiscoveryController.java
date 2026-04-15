package com.jinloes.servicediscovery.springcloudk8s.web;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServiceDiscoveryController {

  private final DiscoveryClient discoveryClient;

  @GetMapping("/hello")
  public String hello() {
    return "Hello from K8s-discovered service";
  }

  // Lists all Kubernetes Services visible to this pod (respects all-namespaces config)
  @GetMapping("/services")
  public List<String> listServices() {
    return discoveryClient.getServices();
  }

  // Returns all pods backing a given K8s Service, with metadata from the K8s API
  @GetMapping("/instances/{serviceId}")
  public List<Map<String, Object>> getInstances(@PathVariable String serviceId) {
    return discoveryClient.getInstances(serviceId).stream()
        .map(ServiceDiscoveryController::toInstanceMap)
        .toList();
  }

  private static Map<String, Object> toInstanceMap(ServiceInstance instance) {
    return Map.of(
        "instanceId", instance.getInstanceId(),
        "host", instance.getHost(),
        "port", instance.getPort(),
        "uri", instance.getUri().toString(),
        // Spring Cloud Kubernetes populates k8s_namespace from the pod's namespace label
        "namespace", instance.getMetadata().getOrDefault("k8s_namespace", "default"));
  }
}
