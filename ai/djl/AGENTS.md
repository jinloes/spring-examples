# djl

Spring Boot service that classifies the programming language of a code snippet using
[Deep Java Library (DJL)](https://djl.ai/) and the
[CodeBERTa-language-id](https://huggingface.co/huggingface/CodeBERTa-language-id) model.

> 📐 Architecture, design, and reference details for this module live in [ARCHITECTURE.md](ARCHITECTURE.md).

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `app.djl.model-path` | `~/.djl.ai/CodeBERTa-language-id` | Path to the model directory |

Override via environment variable or JVM arg:

```bash
DJL_MODEL_PATH=/path/to/model ./gradlew :ai:djl:bootRun
# or
./gradlew :ai:djl:bootRun --args='--app.djl.model-path=/path/to/model'
```

## Running

```bash
./gradlew :ai:djl:downloadModel   # first time only
./gradlew :ai:djl:bootRun
```

### Classify a snippet

```bash
curl -X POST http://localhost:8080/classify \
  -H 'Content-Type: application/json' \
  -d '{"code": "public class Hello { public static void main(String[] args) {} }"}'
```

Response:

```json
{
  "classifications": {
    "java": 0.9998,
    "javascript": 0.0001,
    "python": 0.0001
  }
}
```

## Tests

The controller tests (`CodeClassificationControllerTest`) run without the model — `DjlService`
is replaced with a Mockito mock via `@MockitoBean`, so no model files are required:

```bash
./gradlew :ai:djl:test
```
