# service-discovery

Three sub-modules demonstrating different service discovery approaches with Spring Boot.
Each sub-module exposes the same REST API so the backends are easy to compare.

| Sub-module | Backend | Requires |
|---|---|---|
| `kubernetes` | Native K8s DNS + env vars | Running K8s cluster |
| `consul` | HashiCorp Consul | Consul agent (`docker-compose up`) |
| `spring-cloud-kubernetes` | K8s API via Spring Cloud Kubernetes | Running K8s cluster + RBAC |

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

### kubernetes (no Spring Cloud, K8s DNS only)
```bash
./gradlew :service-discovery:kubernetes:bootRun
```
> The `/api/call/{serviceName}` and `/api/env/{serviceName}` endpoints only work inside a K8s cluster.

### consul
```bash
# Start Consul first
docker-compose -f service-discovery/consul/docker-compose.yml up -d

./gradlew :service-discovery:consul:bootRun
```

### spring-cloud-kubernetes
```bash
# Apply RBAC then deploy (requires a running cluster)
kubectl apply -f service-discovery/spring-cloud-kubernetes/k8s/rbac.yml
kubectl apply -f service-discovery/spring-cloud-kubernetes/k8s/deployment.yml
```

## Tests
```bash
./gradlew :service-discovery:kubernetes:test
./gradlew :service-discovery:consul:test
./gradlew :service-discovery:spring-cloud-kubernetes:test
```

### Testing strategy and rationale

**`spring-cloud-kubernetes`** gets a real K3s integration test (`SpringCloudKubernetesApplicationTest`).
The `DiscoveryClient` calls the **Kubernetes API server**, which we can redirect to K3s by overriding
the `ApiClient` bean with one built from `K3sContainer.getKubeConfigYaml()`. The app runs locally;
the cluster is K3s in Docker. Tests hit the actual `/api/services` and `/api/instances/{id}`
endpoints and verify what K3s contains.

**`consul`** disables Consul on startup (`spring.cloud.consul.*.enabled=false`) so the context loads
without a running agent. A proper integration test would start a Consul container via Testcontainers
and wire the discovery client to it — same pattern as `spring-cloud-kubernetes`.

**`kubernetes`** (native DNS/env vars) has two test classes:
- `KubernetesDiscoveryApplicationTest` — basic context load and `/api/hello` / `/api/env` checks
- `KubernetesEnvVarInjectionTest` — passes `PAYMENT_SERVICE_SERVICE_HOST=...` etc. via
  `@SpringBootTest(properties = {...})` to simulate kubelet env-var injection; Spring's `Environment`
  reads Spring properties and OS env vars from the same source, so this exactly matches runtime behavior

The `/api/call/{serviceName}` endpoint cannot be integration-tested from outside the cluster:
it resolves `<name>.default.svc.cluster.local` via **CoreDNS**, which is only reachable from inside
a K8s pod. Full end-to-end testing of that path requires building a Docker image of the app and
deploying it as a pod in K3s — a deployment pipeline test, not a unit test.
