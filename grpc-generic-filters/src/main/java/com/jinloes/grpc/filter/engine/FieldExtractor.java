package com.jinloes.grpc.filter.engine;

/**
 * Strategy for extracting a field value from a backend-specific record type using a proto
 * dot-notation field path (e.g. {@code "name"}, {@code "address.city"}).
 *
 * <p>Each backend provides its own implementation that maps proto field paths to its internal field
 * names, isolating the {@link FilterEngine} from naming convention differences between backends.
 *
 * @param <T> the internal record type of the backend
 */
@FunctionalInterface
public interface FieldExtractor<T> {

  /**
   * Returns the value of the field identified by {@code fieldPath} on {@code record}. Throws {@link
   * IllegalArgumentException} for unknown paths.
   */
  Object getField(T record, String fieldPath);
}
