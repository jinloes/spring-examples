# grpc/generic-filters — Architecture

Reference and design details for `grpc/generic-filters`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Architecture

```
grpcurl / gRPC client
    │
    ▼
PersonFilterServiceImpl
    │
    ├── FilterRequestValidator     (checks field + operator against FilterCapabilityRegistry)
    ├── InMemoryPersonRepository   (owns FilterEngine<PersonModel>  + FieldExtractorBuilder)
    └── AlternativePersonRepository(owns FilterEngine<PersonRecord> + FieldExtractorBuilder)
```

**`FilterCapabilityRegistry`** — walks the `Person` proto descriptor at startup; any field
annotated with `(filteropts.filter_opts)` in `person.proto` becomes filterable automatically.

**Backends** — the `backend` field in `FilterRequest` (enum `Backend`) selects which repository
handles the request. Each backend stores its own internal model with its own field names and owns
a dedicated `FilterEngine<T>` wired with a `FieldExtractorBuilder` that maps proto field paths
(via proto-generated field-number constants) to internal accessor functions. Path resolution is
validated against the proto descriptor at startup; unknown field numbers fail immediately.

| Backend enum value | Repository | Internal model |
|--------------------|------------|----------------|
| `IN_MEMORY` (default) | `InMemoryPersonRepository` | `PersonModel` — field names mirror the proto |
| `ALTERNATIVE` | `AlternativePersonRepository` | `PersonRecord` — different field names (`fullName`, `yearsOld`, `team`, …) |

## Proto files

| File | Purpose |
|------|---------|
| `filters.proto` | `FilterRequest`, `FieldFilter`, `FilterOperator`, `LogicalOperator`, `Backend`, `FieldFilterCapability` |
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
