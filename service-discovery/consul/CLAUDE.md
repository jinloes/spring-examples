# service-discovery/consul

Demonstrates **HashiCorp Consul service discovery** using Spring Cloud Consul. On startup the
app self-registers with the Consul agent and obtains a Spring Cloud `DiscoveryClient` backed by
Consul's catalog. The controller uses `DiscoveryClient` to list registered services and resolve
their live instances — no hardcoded addresses needed.

See the parent [`service-discovery/CLAUDE.md`](../CLAUDE.md) for a comparison of all three
sub-modules.

## Prerequisites

A running Consul agent is required. The included `docker-compose.yml` starts a single-node
Consul server (no external installation needed):

```bash
docker-compose -f service-discovery/consul/docker-compose.yml up -d
```

This starts `hashicorp/consul:1.20` with:
- HTTP API + UI on `localhost:8500`
- DNS on `localhost:8600/udp`

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

## Configuration

`src/main/resources/application.yml`:

| Key | Value | Notes |
|---|---|---|
| `spring.application.name` | `consul-service-discovery` | Service name registered in Consul |
| `spring.cloud.consul.host` | `localhost` | Consul agent address |
| `spring.cloud.consul.port` | `8500` | Consul agent HTTP port |
| `spring.cloud.consul.discovery.health-check-path` | `/actuator/health` | Consul polls this to mark the instance healthy |
| `spring.cloud.consul.discovery.health-check-interval` | `15s` | How often Consul checks health |
| `spring.cloud.consul.discovery.instance-id` | `${spring.application.name}-${random.value}` | Unique ID per instance |
| `server.port` | `8080` | |

## Dependencies

- `spring-cloud-starter-consul-discovery` (Spring Cloud `2025.1.1` BOM)

## How to run

```bash
# Start Consul first
docker-compose -f service-discovery/consul/docker-compose.yml up -d

# Run the app
./gradlew :service-discovery:consul:bootRun
```

The Consul UI is available at `http://localhost:8500`. After the app starts you will see
`consul-service-discovery` appear as a registered service.

## API endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/hello` | Returns a static greeting |
| `GET` | `/api/services` | Lists all service names from the Consul catalog |
| `GET` | `/api/instances/{serviceId}` | Returns host, port, URI, and metadata for each live instance |

## How to test

```bash
./gradlew :service-discovery:consul:test
```

### Test class

| Class | What it tests |
|---|---|
| `ConsulDiscoveryApplicationTest` | Verifies the Spring context loads cleanly without a running Consul agent by disabling all Consul integration (`spring.cloud.consul.enabled=false`, `spring.cloud.consul.config.enabled=false`, `spring.cloud.consul.discovery.enabled=false`, `spring.cloud.consul.discovery.register=false`) |

A proper integration test would start a Consul container via Testcontainers, configure the
discovery client to point at it, and assert that registered services appear in the API
responses — the same pattern used in the `spring-cloud-kubernetes` sub-module with K3s.
