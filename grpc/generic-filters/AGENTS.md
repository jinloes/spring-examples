# grpc/generic-filters

Demonstrates generic protobuf-based field filtering over gRPC, with operator constraints declared
directly in the proto source so any client that imports the proto sees them inline.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Port

gRPC server: **9092** (plaintext)

## Running

```bash
./gradlew :grpc:generic-filters:bootRun
```

## grpcurl examples

Requires [grpcurl](https://github.com/fullstorydev/grpcurl) and the server running on port 9092.

The `"backend"` field is optional; it defaults to `IN_MEMORY` when omitted.

### List all persons (no filters, default backend)
```bash
grpcurl -plaintext \
  -d '{}' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### List all persons from the alternative backend
```bash
grpcurl -plaintext \
  -d '{ "backend": "ALTERNATIVE" }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Filter by city (EQUALS)
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "address.city", "operator": "EQUALS", "stringValue": "San Francisco" }
    ]
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Filter by city on the alternative backend
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "address.city", "operator": "EQUALS", "stringValue": "San Francisco" }
    ],
    "backend": "ALTERNATIVE"
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Filter by age range (AND)
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "age", "operator": "GREATER_THAN",       "intValue": 28 },
      { "fieldPath": "age", "operator": "LESS_THAN_OR_EQUAL", "intValue": 40 }
    ],
    "logicalOperator": "AND"
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Filter by department membership (IN)
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      {
        "fieldPath": "department",
        "operator": "IN",
        "stringListValue": { "values": ["Engineering", "Finance"] }
      }
    ]
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Active Engineering employees earning over $90k (multi-field AND)
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "department", "operator": "EQUALS",       "stringValue": "Engineering" },
      { "fieldPath": "salary",     "operator": "GREATER_THAN", "doubleValue": 90000 },
      { "fieldPath": "active",     "operator": "EQUALS",       "boolValue": true }
    ],
    "logicalOperator": "AND"
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Same query on the alternative backend
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "department", "operator": "EQUALS",       "stringValue": "Engineering" },
      { "fieldPath": "salary",     "operator": "GREATER_THAN", "doubleValue": 90000 },
      { "fieldPath": "active",     "operator": "EQUALS",       "boolValue": true }
    ],
    "logicalOperator": "AND",
    "backend": "ALTERNATIVE"
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Name substring match (case-insensitive CONTAINS)
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "name", "operator": "CONTAINS", "stringValue": "son" }
    ]
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### OR — persons in Austin or Seattle
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "address.city", "operator": "EQUALS", "stringValue": "Austin" },
      { "fieldPath": "address.city", "operator": "EQUALS", "stringValue": "Seattle" }
    ],
    "logicalOperator": "OR"
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

### Error — disallowed operator (returns INVALID_ARGUMENT)
```bash
grpcurl -plaintext \
  -d '{
    "filters": [
      { "fieldPath": "active", "operator": "GREATER_THAN", "boolValue": true }
    ]
  }' \
  localhost:9092 \
  person.PersonFilterService/ListPersons
```

## Adding a new filterable field

1. Annotate the field in `person.proto` (or a nested message) with `(filteropts.filter_opts)`,
   listing `allowed_operators` and a `description`.
2. Run `./gradlew :grpc:generic-filters:generateProto` (or `bootRun` / `test`, which trigger it).
3. Bind the new field in **each backend's** `FieldExtractorBuilder`:
   - `InMemoryPersonRepository.EXTRACTOR` — add `.bind(Person.NEW_FIELD_NUMBER, PersonModel::newField)`
   - `AlternativePersonRepository` — add the equivalent bind for its internal model accessor
4. `FilterCapabilityRegistry` picks up the proto annotation automatically; no other Java changes required.

## Tests

```bash
./gradlew :grpc:generic-filters:test
```

Tests use an in-process gRPC server (no real network). See:
- `PersonFilterServiceImplTest` — integration tests via the gRPC stub
- `FilterEngineTest` — unit tests for filter evaluation logic
- `FilterCapabilityRegistryTest` — unit tests for descriptor-derived capabilities
- `BackendFieldMappingTest` — parameterized test verifying every filterable proto field has a
  mapping in both `InMemoryPersonRepository` and `AlternativePersonRepository`; adding a proto
  annotation without binding it in a backend will fail these tests immediately
