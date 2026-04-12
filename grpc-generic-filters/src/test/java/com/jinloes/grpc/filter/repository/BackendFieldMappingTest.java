package com.jinloes.grpc.filter.repository;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.jinloes.grpc.filter.capability.FilterCapabilityRegistry;
import com.jinloes.grpc.filter.proto.FieldFilter;
import com.jinloes.grpc.filter.proto.FilterOperator;
import com.jinloes.grpc.filter.proto.FilterRequest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies that every proto field with at least one operator defined in {@code person.proto} has a
 * corresponding mapping in each backend's {@link com.jinloes.grpc.filter.engine.FieldExtractor}.
 *
 * <p>Adding a new {@code (filteropts.filter_opts)} annotation to a proto field without also binding
 * it in a backend will cause these tests to fail, surfacing the gap immediately.
 */
class BackendFieldMappingTest {

  private static final FilterCapabilityRegistry REGISTRY = new FilterCapabilityRegistry();

  static Stream<String> filterableFieldPaths() {
    return REGISTRY.getAll().stream().map(c -> c.getFieldPath());
  }

  @ParameterizedTest(name = "in-memory backend: {0}")
  @MethodSource("filterableFieldPaths")
  void inMemoryBackend_hasMappingFor(String fieldPath) {
    assertMapped(new InMemoryPersonRepository(), fieldPath);
  }

  @ParameterizedTest(name = "alternative backend: {0}")
  @MethodSource("filterableFieldPaths")
  void alternativeBackend_hasMappingFor(String fieldPath) {
    assertMapped(new AlternativePersonRepository(), fieldPath);
  }

  /**
   * Probes the backend with an EQUALS filter on {@code fieldPath}. A missing mapping throws
   * "Unknown field path: …"; a present-but-non-matching mapping simply returns an empty list. Type
   * mismatches on the probe value also return false rather than throwing, so the only failure mode
   * caught here is a missing field mapping.
   */
  private static void assertMapped(PersonRepository repository, String fieldPath) {
    FilterRequest probe =
        FilterRequest.newBuilder()
            .addFilters(
                FieldFilter.newBuilder()
                    .setFieldPath(fieldPath)
                    .setOperator(FilterOperator.EQUALS)
                    .setStringValue("__probe__")
                    .build())
            .build();

    assertThatCode(() -> repository.filter(probe))
        .as(
            "%s must have a mapping for filterable field '%s'",
            repository.getClass().getSimpleName(), fieldPath)
        .doesNotThrowAnyException();
  }
}
