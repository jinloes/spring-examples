# grpc-generic-filters

Demonstrates generic protobuf-based field filtering over gRPC, with operator constraints declared
directly in the proto source so any client that imports the proto sees them inline.

## Port

gRPC server: **9092** (plaintext)

## Running

```bash
./gradlew :grpc-generic-filters:bootRun
```

## Architecture

```
grpcurl / gRPC client
    │
    ▼
PersonFilterServiceImpl
    │
    ├── FilterRequestValidator  (checks field + operator against FilterCapabilityRegistry)
    └── FilterEngine            (generic <T extends Message> dot-notation traversal)
```

**`FilterCapabilityRegistry`** — walks the `Person` proto descriptor at startup; any field
annotated with `(filteropts.filter_opts)` in `person.proto` becomes filterable automatically.
No Java change is needed when adding a new filterable field — only a proto annotation.

## Proto files

| File | Purpose |
|------|---------|
| `filters.proto` | `FilterRequest`, `FieldFilter`, `FilterOperator`, `LogicalOperator`, `FieldFilterCapability` |
| `filter_options.proto` | Custom `FieldOptions` extension that declares allowed operators per field |
| `person.proto` | `PersonFilterService` RPC + `Person` model with per-field operator annotations |

## Filterable fields (from `person.proto`)

| Field path | Type | Allowed operators |
|------------|------|-------------------|
| `id` | string | EQUALS, IN, NOT_IN |
| `name` | string | EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, IN, NOT_IN |
| `age` | int32 | EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, IN, NOT_IN |
| `department` | string | EQUALS, NOT_EQUALS, IN, NOT_IN |
| `salary` | double | EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL |
| `active` | bool | EQUALS, NOT_EQUALS |
| `address.street` | string | EQUALS, CONTAINS, STARTS_WITH |
| `address.city` | string | EQUALS, NOT_EQUALS, CONTAINS, IN, NOT_IN |
| `address.state` | string | EQUALS, NOT_EQUALS, IN, NOT_IN |
| `address.zip` | string | EQUALS, IN, NOT_IN |
| `address.country` | string | EQUALS, NOT_EQUALS, IN, NOT_IN |
| `contact.email` | string | EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH |
| `contact.phone` | string | EQUALS, STARTS_WITH |

String comparisons (CONTAINS, STARTS_WITH, ENDS_WITH) are **case-insensitive**.
IN / NOT_IN for numeric fields use string representation (e.g. `"30"` for age 30).

## grpcurl examples

Requires [grpcurl](https://github.com/fullstorydev/grpcurl) and the server running on port 9092.

### List all persons (no filters)
```bash
grpcurl -plaintext \
  -d '{}' \
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
2. Run `./gradlew :grpc-generic-filters:generateProto` (or `bootRun` / `test`, which trigger it).
3. No Java changes required — `FilterCapabilityRegistry` picks up the annotation automatically.

## Tests

```bash
./gradlew :grpc-generic-filters:test
```

Tests use an in-process gRPC server (no real network). See:
- `PersonFilterServiceImplTest` — integration tests via the gRPC stub
- `FilterCapabilityRegistryTest` — unit tests for descriptor-derived capabilities