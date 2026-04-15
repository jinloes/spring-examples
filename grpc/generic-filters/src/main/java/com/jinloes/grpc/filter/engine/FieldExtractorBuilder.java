package com.jinloes.grpc.filter.engine;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds a {@link FieldExtractor} backed by a map of accessor functions, using proto-generated
 * field number constants as keys instead of hard-coded strings.
 *
 * <p>Field numbers are validated against the proto {@link Descriptor} at build time: a field number
 * that does not exist in the descriptor throws immediately, catching stale mappings at startup
 * rather than at query time.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * import static com.jinloes.grpc.filter.engine.FieldExtractorBuilder.path;
 *
 * FieldExtractor<PersonRecord> extractor =
 *     FieldExtractorBuilder.<PersonRecord>forProto(Person.getDescriptor())
 *         .bind(Person.NAME_FIELD_NUMBER,     PersonRecord::fullName)
 *         .bind(Person.AGE_FIELD_NUMBER,      r -> r.yearsOld())
 *         .bind(path(Person.ADDRESS_FIELD_NUMBER, Address.CITY_FIELD_NUMBER),
 *               r -> r.location().cityName())
 *         .build();
 * }</pre>
 *
 * @param <T> the internal record type of the backend
 */
public final class FieldExtractorBuilder<T> {

  private final Descriptor protoDescriptor;
  private final Map<String, Function<T, Object>> fields = new LinkedHashMap<>();

  private FieldExtractorBuilder(Descriptor protoDescriptor) {
    this.protoDescriptor = protoDescriptor;
  }

  /**
   * Creates a new builder that resolves and validates field numbers against {@code
   * protoDescriptor}.
   *
   * @param protoDescriptor the top-level proto descriptor (e.g. {@code Person.getDescriptor()})
   */
  public static <T> FieldExtractorBuilder<T> forProto(Descriptor protoDescriptor) {
    return new FieldExtractorBuilder<>(protoDescriptor);
  }

  /**
   * Creates a {@link FieldPath} for a nested field. Each number identifies a field at the
   * corresponding nesting level of the proto message.
   *
   * <pre>{@code path(Person.ADDRESS_FIELD_NUMBER, Address.CITY_FIELD_NUMBER)}</pre>
   */
  public static FieldPath path(int... fieldNumbers) {
    return new FieldPath(fieldNumbers);
  }

  /**
   * Binds a top-level proto field (identified by its generated field number constant) to an
   * accessor function on {@code T}.
   */
  public FieldExtractorBuilder<T> bind(int fieldNumber, Function<T, Object> accessor) {
    String resolvedPath = resolvePath(protoDescriptor, new int[] {fieldNumber});
    fields.put(resolvedPath, accessor);
    return this;
  }

  /**
   * Binds a nested proto field path (identified by field numbers at each nesting level) to an
   * accessor function on {@code T}. Use {@link #path(int...)} to create the path.
   */
  public FieldExtractorBuilder<T> bind(FieldPath fieldPath, Function<T, Object> accessor) {
    String resolvedPath = resolvePath(protoDescriptor, fieldPath.numbers());
    fields.put(resolvedPath, accessor);
    return this;
  }

  /**
   * Builds an immutable {@link FieldExtractor} from the registered bindings. Field lookup at filter
   * time is O(1) via map.
   */
  public FieldExtractor<T> build() {
    Map<String, Function<T, Object>> immutable = Map.copyOf(fields);
    return (record, path) -> {
      Function<T, Object> fn = immutable.get(path);
      if (fn == null) {
        throw new IllegalArgumentException("Unknown field path: " + path);
      }
      return fn.apply(record);
    };
  }

  /**
   * Resolves a sequence of field numbers to a dot-notation path string by traversing the proto
   * descriptor tree. Throws if a field number is not found or a non-message field is traversed
   * mid-path.
   */
  private static String resolvePath(Descriptor descriptor, int[] fieldNumbers) {
    StringBuilder path = new StringBuilder();
    Descriptor current = descriptor;
    for (int i = 0; i < fieldNumbers.length; i++) {
      FieldDescriptor field = current.findFieldByNumber(fieldNumbers[i]);
      if (field == null) {
        throw new IllegalArgumentException(
            "Field number %d not found in message '%s'"
                .formatted(fieldNumbers[i], current.getName()));
      }
      if (i > 0) {
        path.append('.');
      }
      path.append(field.getName());
      if (i < fieldNumbers.length - 1) {
        if (field.getType() != FieldDescriptor.Type.MESSAGE) {
          throw new IllegalArgumentException(
              "Field '%s' in '%s' is not a message type and cannot be traversed"
                  .formatted(field.getName(), current.getName()));
        }
        current = field.getMessageType();
      }
    }
    return path.toString();
  }

  /** A sequence of proto field numbers identifying a nested field path. */
  public record FieldPath(int[] numbers) {}
}
