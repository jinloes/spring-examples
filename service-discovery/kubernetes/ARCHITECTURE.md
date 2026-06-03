# service-discovery/kubernetes — Architecture

Reference and design details for `service-discovery/kubernetes`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

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
