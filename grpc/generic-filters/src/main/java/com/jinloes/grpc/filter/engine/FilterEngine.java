package com.jinloes.grpc.filter.engine;

import com.jinloes.grpc.filter.proto.FieldFilter;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.LogicalOperator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Evaluates {@link FilterRequest} predicates against a list of backend-specific records.
 *
 * <p>Field access is delegated to a {@link FieldExtractor} supplied at construction time, so the
 * engine itself is completely independent of any particular data model or naming convention. Each
 * backend instantiates its own {@code FilterEngine} with the extractor that knows how to map proto
 * field paths to its own internal fields.
 *
 * @param <T> the internal record type of the backend
 */
@Slf4j
public class FilterEngine<T> {

  private final FieldExtractor<T> extractor;

  public FilterEngine(FieldExtractor<T> extractor) {
    this.extractor = extractor;
  }

  /**
   * Returns the subset of {@code items} that satisfy all (AND) or any (OR) filters in {@code
   * request}. Returns all items when the filter list is empty.
   */
  public List<T> apply(List<T> items, FilterRequest request) {
    if (request.getFiltersList().isEmpty()) {
      log.debug("No filters specified — returning all {} item(s)", items.size());
      return items;
    }

    List<T> result = items.stream().filter(item -> evaluate(item, request)).toList();

    log.debug(
        "{} of {} item(s) matched ({} filter(s), operator={})",
        result.size(),
        items.size(),
        request.getFiltersCount(),
        request.getLogicalOperator());
    return result;
  }

  private boolean evaluate(T item, FilterRequest request) {
    List<FieldFilter> filters = request.getFiltersList();
    if (request.getLogicalOperator() == LogicalOperator.OR) {
      return filters.stream().anyMatch(f -> matchesFilter(item, f));
    }
    // Default: AND — every filter must match
    return filters.stream().allMatch(f -> matchesFilter(item, f));
  }

  private boolean matchesFilter(T item, FieldFilter filter) {
    Object value = extractor.getField(item, filter.getFieldPath());
    return switch (filter.getOperator()) {
      case EQUALS -> checkEquals(value, filter);
      case NOT_EQUALS -> !checkEquals(value, filter);
      case GREATER_THAN -> compare(value, filter) > 0;
      case LESS_THAN -> compare(value, filter) < 0;
      case GREATER_THAN_OR_EQUAL -> compare(value, filter) >= 0;
      case LESS_THAN_OR_EQUAL -> compare(value, filter) <= 0;
      case CONTAINS -> checkContains(value, filter);
      case STARTS_WITH -> checkStartsWith(value, filter);
      case ENDS_WITH -> checkEndsWith(value, filter);
      case IN -> checkIn(value, filter);
      case NOT_IN -> !checkIn(value, filter);
      default ->
          throw new UnsupportedOperationException(
              "Unsupported filter operator: %s".formatted(filter.getOperator()));
    };
  }

  private boolean checkEquals(Object fieldValue, FieldFilter filter) {
    return switch (filter.getValueCase()) {
      case STRING_VALUE -> String.valueOf(fieldValue).equals(filter.getStringValue());
      case INT_VALUE -> {
        // Proto int32 → Integer, int64 → Long; both compared as long
        if (fieldValue instanceof Integer i) yield i.longValue() == filter.getIntValue();
        if (fieldValue instanceof Long l) yield l == filter.getIntValue();
        yield false;
      }
      case DOUBLE_VALUE -> {
        if (fieldValue instanceof Double d) yield Double.compare(d, filter.getDoubleValue()) == 0;
        if (fieldValue instanceof Float f)
          yield Double.compare(f.doubleValue(), filter.getDoubleValue()) == 0;
        yield false;
      }
      case BOOL_VALUE -> fieldValue instanceof Boolean b && b == filter.getBoolValue();
      default -> false;
    };
  }

  /** Returns negative / zero / positive analogous to {@link Comparable#compareTo}. */
  private int compare(Object fieldValue, FieldFilter filter) {
    return switch (filter.getValueCase()) {
      case INT_VALUE -> {
        long fv = fieldValue instanceof Integer i ? i.longValue() : ((Long) fieldValue);
        yield Long.compare(fv, filter.getIntValue());
      }
      case DOUBLE_VALUE -> {
        double fv = fieldValue instanceof Float f ? f.doubleValue() : ((Double) fieldValue);
        yield Double.compare(fv, filter.getDoubleValue());
      }
      case STRING_VALUE -> String.valueOf(fieldValue).compareTo(filter.getStringValue());
      default ->
          throw new IllegalArgumentException(
              "Cannot compare with value type: %s".formatted(filter.getValueCase()));
    };
  }

  /** Case-insensitive substring match. */
  private boolean checkContains(Object fieldValue, FieldFilter filter) {
    if (filter.getValueCase() != FieldFilter.ValueCase.STRING_VALUE) {
      return false;
    }
    return normalize(fieldValue).contains(normalize(filter.getStringValue()));
  }

  private boolean checkStartsWith(Object fieldValue, FieldFilter filter) {
    if (filter.getValueCase() != FieldFilter.ValueCase.STRING_VALUE) {
      return false;
    }
    return normalize(fieldValue).startsWith(normalize(filter.getStringValue()));
  }

  private boolean checkEndsWith(Object fieldValue, FieldFilter filter) {
    if (filter.getValueCase() != FieldFilter.ValueCase.STRING_VALUE) {
      return false;
    }
    return normalize(fieldValue).endsWith(normalize(filter.getStringValue()));
  }

  /** Converts a value to its lowercase string form for case-insensitive comparisons. */
  private String normalize(Object value) {
    return String.valueOf(value).toLowerCase();
  }

  /**
   * Checks membership using the {@code string_list_value}. For numeric fields, provide their string
   * representation (e.g. "30" to match age 30).
   */
  private boolean checkIn(Object fieldValue, FieldFilter filter) {
    if (filter.getValueCase() != FieldFilter.ValueCase.STRING_LIST_VALUE) {
      return false;
    }
    return filter.getStringListValue().getValuesList().contains(String.valueOf(fieldValue));
  }
}
