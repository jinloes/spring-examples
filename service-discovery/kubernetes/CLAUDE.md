# service-discovery/kubernetes

Demonstrates **native Kubernetes service discovery** using only K8s-built-in mechanisms — no
Spring Cloud dependency. The app shows two approaches the Kubernetes runtime provides for free
to every pod:

1. **DNS** — every `Service` object gets a DNS entry `<name>.<namespace>.svc.cluster.local`,
   resolvable from any pod in the cluster via CoreDNS.
2. **Environment variables** — the kubelet injects `<UPPER_SNAKE_SERVICE>_SERVICE_HOST` and
   `<UPPER_SNAKE_SERVICE>_SERVICE_PORT` into every pod for each `ClusterIP` Service in the
   same namespace.

See the parent [`service-discovery/CLAUDE.md`](../CLAUDE.md) for a comparison of all three
sub-modules.

## Prerequisites

Running this module locally requires a Kubernetes cluster.  There is no Docker Compose or
Testcontainers setup for the server itself — the app must be deployed as a pod so that CoreDNS
is reachable and the kubelet injects env vars.

The `/api/hello` and `/api/env` endpoints work outside a cluster (env vars return
`"not-injected"`). The `/api/call/{serviceName}` endpoint **only works inside a pod** — it
resolves `<name>.default.svc.cluster.local` via CoreDNS.

## Key classes

| Class | Role |
|---|---|
| `KubernetesDiscoveryApplication` | `@SpringBootApplication` entry point; declares the `RestTemplate` bean used for DNS calls |
| `ServiceDiscoveryController` | REST controller exposing `/api/hello`, `/api/call/{serviceName}`, and `/api/env/{serviceName}` |

### How discovery is wired

No Spring Cloud `DiscoveryClient` is used. The controller:

- **DNS call** (`/api/call/{serviceName}`): builds the URL
  `http://<serviceName>.default.svc.cluster.local/api/hello` and executes it with
  `RestTemplate`. Resolution succeeds only when CoreDNS is reachable (inside a pod).
- **Env var inspection** (`/api/env/{serviceName}`): upper-cases the name, replaces hyphens
  with underscores, then reads `<KEY>_SERVICE_HOST` and `<KEY>_SERVICE_PORT` from Spring's
  `Environment`. Spring's `Environment` unifies OS env vars and Spring properties, so the
  kubelet-injected env vars are read exactly the same way at runtime.

## Configuration

`src/main/resources/application.yml`:

| Key | Value | Notes |
|---|---|---|
| `spring.application.name` | `kubernetes-discovery` | Returned by `/api/hello` |
| `server.port` | `8080` | Container port matches `k8s/service.yml` `targetPort` |
| `management.endpoints.web.exposure.include` | `health,info` | Used by the readiness probe |

## Kubernetes manifests

| File | Purpose |
|---|---|
| `k8s/deployment.yml` | Deploys 2 replicas; readiness probe hits `/actuator/health` |
| `k8s/service.yml` | `ClusterIP` Service on port 80 → pod 8080; creates the DNS entry `kubernetes-discovery.default.svc.cluster.local` and injects `KUBERNETES_DISCOVERY_SERVICE_HOST` / `KUBERNETES_DISCOVERY_SERVICE_PORT` into all pods in the namespace |

## How to run

### Deploy to a cluster

```bash
# Build and push the image, then apply manifests
kubectl apply -f service-discovery/kubernetes/k8s/deployment.yml
kubectl apply -f service-discovery/kubernetes/k8s/service.yml
```

### Local (limited functionality)

```bash
./gradlew :service-discovery:kubernetes:bootRun
```

`/api/hello` and `/api/env` work locally. `/api/call/{serviceName}` will fail — CoreDNS is
only reachable from inside a pod.

## How to test

```bash
./gradlew :service-discovery:kubernetes:test
```

### Test classes

| Class | What it tests |
|---|---|
| `KubernetesDiscoveryApplicationTest` | Context load, `/api/hello` returns the app name, `/api/env` returns `"not-injected"` outside a cluster |
| `KubernetesEnvVarInjectionTest` | Passes `PAYMENT_SERVICE_SERVICE_HOST=10.96.42.100` and `PAYMENT_SERVICE_SERVICE_PORT=8080` via `@SpringBootTest(properties = {...})` to simulate kubelet injection; verifies the controller reads them correctly |

`/api/call/{serviceName}` cannot be integration-tested outside a cluster because it depends on
CoreDNS. Testing it end-to-end requires building a Docker image and deploying the app as a pod
in K3s or a real cluster.
