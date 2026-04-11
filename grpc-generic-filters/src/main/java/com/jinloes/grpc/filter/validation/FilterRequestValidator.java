package com.jinloes.grpc.filter.validation;

import com.jinloes.grpc.filter.capability.FilterCapabilityRegistry;
import com.jinloes.grpc.filter.proto.FieldFilter;
import com.jinloes.grpc.filter.proto.FieldFilterCapability;
import com.jinloes.grpc.filter.proto.FilterOperator;
import com.jinloes.grpc.filter.proto.FilterRequest;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link FilterRequest} against the declared capabilities in {@link
 * FilterCapabilityRegistry}.
 *
 * <p>This runs <em>before</em> the {@link com.jinloes.grpc.filter.engine.FilterEngine} so that
 * unknown fields and disallowed operators are caught with a precise error message, rather than
 * failing mid-traversal or silently producing wrong results.
 *
 * <p>Two failure modes:
 *
 * <ul>
 *   <li>Unknown field path → {@link InvalidFilterException} listing all valid paths
 *   <li>Disallowed operator → {@link InvalidFilterException} listing the permitted operators for
 *       that field
 * </ul>
 */
@Component
public class FilterRequestValidator {

  private final FilterCapabilityRegistry registry;
  // Built once at startup; reused verbatim in every "unknown field" error message
  private final String validFieldPaths;
  // Allowed-operators description keyed by field path; built once per field at startup
  private final Map<String, String> allowedOperatorsByPath;

  public FilterRequestValidator(FilterCapabilityRegistry registry) {
    this.registry = registry;
    this.validFieldPaths =
        registry.getAll().stream()
            .map(FieldFilterCapability::getFieldPath)
            .sorted()
            .collect(Collectors.joining(", "));
    this.allowedOperatorsByPath =
        registry.getAll().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    FieldFilterCapability::getFieldPath,
                    c ->
                        c.getAllowedOperatorsList().stream()
                            .map(FilterOperator::name)
                            .sorted()
                            .collect(Collectors.joining(", "))));
  }

  /**
   * @throws InvalidFilterException if any filter in the request is invalid
   */
  public void validate(FilterRequest request) {
    for (FieldFilter filter : request.getFiltersList()) {
      String path = filter.getFieldPath();

      FieldFilterCapability capability =
          registry
              .find(path)
              .orElseThrow(
                  () ->
                      new InvalidFilterException(
                          "Field '%s' is not filterable. Valid fields: %s"
                              .formatted(path, validFieldPaths)));

      FilterOperator op = filter.getOperator();
      if (!capability.getAllowedOperatorsList().contains(op)) {
        throw new InvalidFilterException(
            "Operator %s is not allowed for field '%s' (type=%s). Allowed operators: %s"
                .formatted(
                    op.name(), path, capability.getFieldType(), allowedOperatorsByPath.get(path)));
      }
    }
  }
}
