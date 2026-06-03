# grpc/react-dynamic-components

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

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## How to Run

```bash
./gradlew :grpc:react-dynamic-components:bootRun
```

Open `http://localhost:8080`. Use the preset buttons or type component IDs to load forms.

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
