# grpc-react-dynamic-ui

> **For Claude**: Keep this file up to date whenever you change this module. Specifically:
> - Update the API endpoints table when adding/removing endpoints
> - Update the proto section when changing `ui_component.proto`
> - Update the component config section when changing YAML component definitions
> - Update the architecture section when the data flow changes
> - **After every code change, add or update tests.** New RPC methods → unit test in
>   `UiComponentServiceImplTest`. New REST endpoints → integration test in
>   `UiComponentControllerTest`. Use `@Nested` to group tests by method/scenario.

A backend-driven dynamic UI demo. The backend (gRPC + Spring Boot) defines UI component
schemas in YAML. A React frontend fetches those schemas and renders forms dynamically.
Field visibility is evaluated server-side — the frontend POSTs current field values and the
backend responds with only the currently visible fields. The client renders whatever it receives
with no visibility logic of its own.

## How to Run

```bash
./gradlew :grpc-react-dynamic-ui:bootRun
```

Open `http://localhost:8080`. Use the preset buttons or type component IDs to load forms.

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

## Adding a Component

1. Create `src/main/resources/components/<id>.yml`:

```yaml
type: form
fields:
  - name: status
    label: Status
    type: select
    values: [ "Open", "Closed" ]
  - name: notes
    label: Notes
    type: textarea
    visible:
      field: status
      equals: "Open"
```

2. Add a renderer in `index.html` under `COMPONENT_ID_RENDERERS` if you need custom
   action buttons; otherwise the default `ComponentCard` handles it.

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
