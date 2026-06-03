# djl

Spring Boot service that classifies the programming language of a code snippet using
[Deep Java Library (DJL)](https://djl.ai/) and the
[CodeBERTa-language-id](https://huggingface.co/huggingface/CodeBERTa-language-id) model.

## Model

The service uses the HuggingFace `huggingface/CodeBERTa-language-id` model — a RoBERTa-based
classifier that identifies programming languages (Java, Python, JavaScript, Go, Ruby, PHP, C++).

The model must be available locally before the application starts. It is **not** bundled in the
repository (the `pytorch_model.bin` file is ~476 MB).

### Download with Gradle (recommended)

```bash
./gradlew :ai:djl:downloadModel
```

This downloads all required model files from HuggingFace Hub into
`~/.djl.ai/CodeBERTa-language-id/`. Files that already exist are skipped, so it is safe to
re-run.

Files downloaded:
- `config.json`
- `merges.txt`
- `special_tokens_map.json`
- `tokenizer_config.json`
- `vocab.json`
- `pytorch_model.bin` (~476 MB, resolved via HuggingFace LFS)

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

## Architecture

- `DjlService` loads the `ZooModel` once at startup (thread-safe, expensive to create).
- A new `Predictor` is created per request and closed immediately after use; predictors are
  not thread-safe.
- Empty/null input short-circuits before inference and returns an empty classification map.