# service-discovery/spring-cloud-kubernetes

Demonstrates **Spring Cloud Kubernetes service discovery**. Unlike the `kubernetes` sub-module
(which uses K8s DNS and env vars), this module uses the official Kubernetes Java client to query
the **Kubernetes API server** directly. Spring Cloud Kubernetes auto-configures a `DiscoveryClient`
bean that translates K8s `Service` and `Endpoints` objects into `ServiceInstance` records,
giving the same Spring Cloud abstraction used by Consul, Eureka, etc.

See the parent [`service-discovery/CLAUDE.md`](../CLAUDE.md) for a comparison of all three
sub-modules.

## Prerequisites

- A running Kubernetes cluster is required at runtime. The app needs network access to the K8s
  API server and a `ServiceAccount` bound to the RBAC role in `k8s/rbac.yml`.
- **Tests** use Testcontainers K3s (`rancher/k3s:v1.30.0-k3s1`) — no external cluster is needed
  to run `./gradlew :service-discovery:spring-cloud-kubernetes:test`. Docker must be running.

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

## Configuration

`src/main/resources/application.yml`:

| Key | Value | Notes |
|---|---|---|
| `spring.application.name` | `spring-cloud-kubernetes-discovery` | |
| `spring.cloud.kubernetes.discovery.enabled` | `true` | Activates the `DiscoveryClient` |
| `spring.cloud.kubernetes.discovery.all-namespaces` | `false` | Limits discovery to the pod's own namespace; set `true` for cluster-wide discovery (requires `ClusterRole`) |
| `spring.cloud.kubernetes.config.enabled` | `false` | ConfigMap/Secret config import disabled — only discovery is demonstrated |
| `server.port` | `8080` | |

## RBAC

`k8s/rbac.yml` creates:
- `ServiceAccount` `spring-cloud-kubernetes` in the `default` namespace
- `ClusterRole` with `get`, `list`, `watch` on `services`, `endpoints`, `pods`, `namespaces`
- `ClusterRoleBinding` attaching the role to the service account

The `Deployment` sets `serviceAccountName: spring-cloud-kubernetes` so the pod's token grants
the above permissions.

## Kubernetes manifests

| File | Purpose |
|---|---|
| `k8s/rbac.yml` | ServiceAccount, ClusterRole, ClusterRoleBinding — apply before the deployment |
| `k8s/deployment.yml` | 1 replica using the `spring-cloud-kubernetes` ServiceAccount; readiness probe at `/actuator/health` |

## How to run

### Deploy to a cluster

```bash
kubectl apply -f service-discovery/spring-cloud-kubernetes/k8s/rbac.yml
kubectl apply -f service-discovery/spring-cloud-kubernetes/k8s/deployment.yml
```

### Local (against current kubeconfig)

```bash
./gradlew :service-discovery:spring-cloud-kubernetes:bootRun
```

When run locally the Kubernetes client reads `~/.kube/config`. Discovery will query whichever
cluster `kubectl` currently points at. If no kubeconfig is available the context will fail to
load unless discovery is disabled via properties.

## API endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hello` | Returns a static greeting |
| `GET` | `/api/services` | Lists all K8s Service names in the namespace |
| `GET` | `/api/instances/{serviceId}` | Returns host, port, URI, and `k8s_namespace` for each backing pod endpoint |

## How to test

```bash
./gradlew :service-discovery:spring-cloud-kubernetes:test
```

Docker must be running — the test starts a K3s container.

### Test class

`SpringCloudKubernetesApplicationTest` is a real integration test against K3s in Docker:

1. `@Testcontainers` starts `rancher/k3s:v1.30.0-k3s1` before Spring loads.
2. A `@TestConfiguration` inner class (`K3sClientConfig`) overrides the auto-configured
   `ApiClient` bean with one built from `K3S.getKubeConfigYaml()`, redirecting all cluster
   communication to the K3s container.
3. `@BeforeAll` creates a `test-service` ClusterIP Service inside K3s so the discovery
   endpoints return more than just the cluster default.
4. Tests assert that `/api/services` includes both `"kubernetes"` (K3s bootstrap service) and
   `"test-service"`, and that `/api/instances/kubernetes` returns non-empty results.

This approach tests the actual `DiscoveryClient` → Kubernetes API → K3s path with no mocking.
