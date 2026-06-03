# service-discovery/spring-cloud-kubernetes — Architecture

Reference and design details for `service-discovery/spring-cloud-kubernetes`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Key classes

| Class | Role |
|---|---|
| `SpringCloudKubernetesApplication` | `@SpringBootApplication` entry point |
| `ServiceDiscoveryController` | REST controller; injects `DiscoveryClient` to list services and instances |

### How discovery is wired

Spring Cloud Kubernetes (`spring-cloud-starter-kubernetes-client`) auto-configures:

1. An `ApiClient` bean (Kubernetes Java client) from the in-cluster kubeconfig
   (`/var/run/secrets/kubernetes.io/serviceaccount/token`) when running inside a pod, or
   from `~/.kube/config` locally.
2. A `DiscoveryClient` bean backed by `CoreV1Api` that calls `listNamespacedService` /
   `listNamespacedEndpoints` on every lookup.

The controller calls:
- `discoveryClient.getServices()` — lists all `Service` names in the configured namespace
- `discoveryClient.getInstances(serviceId)` — resolves a service name to its backing pod
  endpoints, including `k8s_namespace` from pod metadata

## API endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hello` | Returns a static greeting |
| `GET` | `/api/services` | Lists all K8s Service names in the namespace |
| `GET` | `/api/instances/{serviceId}` | Returns host, port, URI, and `k8s_namespace` for each backing pod endpoint |
