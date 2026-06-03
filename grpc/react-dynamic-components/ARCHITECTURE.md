# grpc/react-dynamic-components — Architecture

Reference and design details for `grpc/react-dynamic-components`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## Architecture

```
Browser (React)
  |
  | GET /api/components?ids=...     → fetch component schemas (fields + types)
  | POST /api/components/state      → send field values, get back resolved visibility
  |
Spring MVC (REST layer)
  |
  | getComponents() → gRPC call → UiComponentServiceImpl
  | resolveState()  → ComponentStateService (evaluates visibility conditions)
  |
gRPC server (port 9091)
  └── UiComponentServiceImpl
        └── loads component YAML files from src/main/resources/components/
```

On every field change the frontend POSTs the full field state. The backend evaluates each
field's visibility condition and returns `{componentId, fieldVisibility: {fieldName: boolean}}`.
The frontend hides any field where `fieldVisibility[name] === false`.

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/components?ids=a,b` | Fetch component schemas by ID |
| `POST` | `/api/components/state` | Resolve field visibility server-side |

### POST /api/components/state

Request:
```json
{ "componentId": "reroute-case", "fieldValues": { "rerouteTo": "Different Support Team" } }
```

Response:
```json
{ "componentId": "reroute-case", "fields": [ { "name": "rerouteTo", ... }, { "name": "caseType", ... } ] }
```
Only fields whose visibility conditions evaluate to true are included. Hidden fields are omitted entirely.

## Proto (`src/main/proto/ui_component.proto`)

```
UiComponentService.GetComponents(ComponentRequest) → ComponentsResponse
UiComponentService.ResolveState(StateRequest) → StateResponse

Field { name, label, type, values[], required }
// Visibility conditions are server-internal (stored in FieldConfig/conditionRegistry),
// not exposed in the proto. Clients receive only the fields that should be shown.
```

## Field Types

| Type | Renders as |
|---|---|
| `text` | `<input type="text">` |
| `password` | `<input type="password">` |
| `textarea` | `<textarea>` |
| `select` | `<select>` — `values` are the options |
| `checkbox` | `<input type="checkbox">` |
| `button` | action button(s) — `values` are the button labels |

## Visibility Condition Operators

| Operator | Meaning |
|---|---|
| `equals: value` | show when `field == value` |
| `notEquals: value` | show when `field != value` |
| `in: [a, b]` | show when `field` is one of the list |
| `notIn: [a, b]` | show when `field` is not in the list |
