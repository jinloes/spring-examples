# service-discovery — Architecture

Reference and design details for `service-discovery`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## API Endpoints (all sub-modules)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hello` | Returns a greeting from this service |
| `GET` | `/api/services` | Lists all discovered services (consul, spring-cloud-kubernetes) |
| `GET` | `/api/instances/{serviceId}` | Lists instances of a service (consul, spring-cloud-kubernetes) |
| `GET` | `/api/call/{serviceName}` | Calls another service via K8s DNS (kubernetes only) |
| `GET` | `/api/env/{serviceName}` | Shows K8s-injected env vars for a service (kubernetes only) |

## Architecture Comparison

### kubernetes sub-module
No Spring Cloud dependency. Relies on two K8s-native mechanisms:
- **DNS**: Every `Service` object gets a DNS entry `<name>.<namespace>.svc.cluster.local`
- **Env vars**: K8s injects `<SERVICE>_SERVICE_HOST` and `<SERVICE>_SERVICE_PORT` into every pod

### consul sub-module
Uses Spring Cloud Consul. On startup, the app registers itself with the Consul agent at
`localhost:8500`. The `DiscoveryClient` bean queries Consul to resolve service IDs to live instances.

### spring-cloud-kubernetes sub-module
Uses Spring Cloud Kubernetes. The `DiscoveryClient` bean queries the Kubernetes API server directly
(via the official K8s Java client) to list Services and their backing Endpoints. Requires RBAC
permissions — see `k8s/rbac.yml`.
