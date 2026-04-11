package com.jinloes.grpc.filter.capability;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.jinloes.grpc.filter.proto.FieldFilterCapability;
import com.jinloes.grpc.filter.proto.FilterOptionsProto;
import com.jinloes.grpc.filter.proto.FilterOptionsProto.FilterFieldOptions;
import com.jinloes.grpc.filter.proto.Person;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Derives the filter capability list from the {@link Person} proto descriptor at startup.
 *
 * <p>Filterable fields are declared in the proto source via the {@code (filteropts.filter_opts)}
 * custom option (in {@code filter_options.proto}). Any client importing {@code person.proto} sees
 * the allowed operators inline; the binary descriptor also carries the metadata for tools like
 * {@code grpcurl} and {@code buf schema}.
 *
 * <p>Adding a filterable field requires only annotating it in the proto — no Java change here.
 */
@Component
public class FilterCapabilityRegistry {

  private final List<FieldFilterCapability> capabilities;
  private final Map<String, FieldFilterCapability> byPath;

  public FilterCapabilityRegistry() {
    // Reference the extension descriptor to ensure FilterOptionsProto is initialised and its
    // extension registered with protobuf before calling getExtension() on any FieldOptions.
    FilterOptionsProto.filterOpts.hashCode();

    this.capabilities = collectCapabilities(Person.getDescriptor(), "");
    this.byPath =
        capabilities.stream()
            .collect(Collectors.toUnmodifiableMap(FieldFilterCapability::getFieldPath, c -> c));
  }

  /** All filterable fields, in descriptor traversal order. */
  public List<FieldFilterCapability> getAll() {
    return capabilities;
  }

  /** Looks up a capability by exact dot-notation field path (e.g. {@code "address.city"}). */
  public Optional<FieldFilterCapability> find(String fieldPath) {
    return Optional.ofNullable(byPath.get(fieldPath));
  }

  /**
   * Recursively walks the message descriptor. For each field:
   *
   * <ul>
   *   <li>Non-empty {@code filter_opts} option → add as a filterable leaf at the current path.
   *   <li>Non-repeated message with no option → descend with a dot-notation path prefix.
   *   <li>Anything else (repeated, un-annotated scalar) → skip.
   * </ul>
   */
  private List<FieldFilterCapability> collectCapabilities(
      com.google.protobuf.Descriptors.Descriptor descriptor, String prefix) {

    List<FieldFilterCapability> result = new ArrayList<>();

    for (FieldDescriptor field : descriptor.getFields()) {
      String path = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();

      FilterFieldOptions opts = field.getOptions().getExtension(FilterOptionsProto.filterOpts);

      if (!opts.getAllowedOperatorsList().isEmpty()) {
        result.add(
            FieldFilterCapability.newBuilder()
                .setFieldPath(path)
                .setFieldType(toTypeString(field))
                .setDescription(opts.getDescription())
                .addAllAllowedOperators(opts.getAllowedOperatorsList())
                .build());

      } else if (field.getType() == Type.MESSAGE && !field.isRepeated()) {
        result.addAll(collectCapabilities(field.getMessageType(), path));
      }
    }

    return result;
  }

  /** Maps a protobuf field type to the human-readable string used in the REST schema response. */
  private String toTypeString(FieldDescriptor field) {
    return switch (field.getType()) {
      case STRING -> "string";
      case INT32, SINT32, SFIXED32, UINT32, FIXED32 -> "int32";
      case INT64, SINT64, SFIXED64, UINT64, FIXED64 -> "int64";
      case DOUBLE -> "double";
      case FLOAT -> "float";
      case BOOL -> "bool";
      default -> field.getType().name().toLowerCase();
    };
  }
}
