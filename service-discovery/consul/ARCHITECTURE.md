# service-discovery/consul — Architecture

Reference and design details for `service-discovery/consul`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Key classes

| Class | Role |
|---|---|
| `ConsulDiscoveryApplication` | `@SpringBootApplication` entry point |
| `ServiceDiscoveryController` | REST controller; injects `DiscoveryClient` to list services and instances |

### How discovery is wired

Spring Cloud Consul auto-configures a `DiscoveryClient` bean backed by the Consul HTTP API.
No manual client setup is required. The controller calls:

- `discoveryClient.getServices()` — queries the Consul catalog for all registered service names
- `discoveryClient.getInstances(serviceId)` — resolves a service name to its live `ServiceInstance`
  objects (host, port, URI, metadata)

On startup, Spring Cloud Consul also **self-registers** the app under the name
`consul-service-discovery` with a periodic health check at `/actuator/health`. Each instance
gets a unique ID (`${spring.application.name}-${random.value}`) so multiple instances can
register simultaneously without colliding.

## API endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hello` | Returns a static greeting |
| `GET` | `/api/services` | Lists all service names from the Consul catalog |
| `GET` | `/api/instances/{serviceId}` | Returns host, port, URI, and metadata for each live instance |
