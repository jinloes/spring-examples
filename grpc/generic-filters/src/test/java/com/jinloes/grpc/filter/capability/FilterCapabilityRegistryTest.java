package com.jinloes.grpc.filter.capability;

import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.grpc.filter.proto.FieldFilterCapability;
import com.jinloes.grpc.filter.proto.FilterOperator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FilterCapabilityRegistryTest {

  private FilterCapabilityRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new FilterCapabilityRegistry();
  }

  @Test
  void allDeclaredFields_arePresent() {
    assertThat(registry.getAll()).isNotEmpty();
    // Spot-check a sample of expected field paths
    assertThat(registry.getAll())
        .extracting(FieldFilterCapability::getFieldPath)
        .contains(
            "name",
            "age",
            "salary",
            "active",
            "department",
            "address.city",
            "address.state",
            "contact.email",
            "contact.phone");
  }

  @Nested
  class StringFields {

    @Test
    void name_allowsStringOperators() {
      FieldFilterCapability cap = requireField("name");
      assertThat(cap.getFieldType()).isEqualTo("string");
      assertThat(cap.getAllowedOperatorsList())
          .contains(
              FilterOperator.EQUALS,
              FilterOperator.CONTAINS,
              FilterOperator.STARTS_WITH,
              FilterOperator.ENDS_WITH,
              FilterOperator.IN,
              FilterOperator.NOT_IN);
    }

    @Test
    void name_doesNotAllowNumericOperators() {
      FieldFilterCapability cap = requireField("name");
      assertThat(cap.getAllowedOperatorsList())
          .doesNotContain(FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN);
    }

    @Test
    void department_allowsMembershipOperators() {
      FieldFilterCapability cap = requireField("department");
      assertThat(cap.getAllowedOperatorsList())
          .contains(FilterOperator.EQUALS, FilterOperator.IN, FilterOperator.NOT_IN);
    }

    @Test
    void department_doesNotAllowSubstringOperators() {
      FieldFilterCapability cap = requireField("department");
      assertThat(cap.getAllowedOperatorsList())
          .doesNotContain(FilterOperator.CONTAINS, FilterOperator.STARTS_WITH);
    }
  }

  @Nested
  class NumericFields {

    @Test
    void age_allowsAllComparisonOperators() {
      FieldFilterCapability cap = requireField("age");
      assertThat(cap.getFieldType()).isEqualTo("int32");
      assertThat(cap.getAllowedOperatorsList())
          .contains(
              FilterOperator.EQUALS, FilterOperator.NOT_EQUALS,
              FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN,
              FilterOperator.GREATER_THAN_OR_EQUAL, FilterOperator.LESS_THAN_OR_EQUAL,
              FilterOperator.IN, FilterOperator.NOT_IN);
    }

    @Test
    void age_doesNotAllowStringOperators() {
      FieldFilterCapability cap = requireField("age");
      assertThat(cap.getAllowedOperatorsList())
          .doesNotContain(
              FilterOperator.CONTAINS, FilterOperator.STARTS_WITH, FilterOperator.ENDS_WITH);
    }

    @Test
    void salary_allowsNumericComparisonButNotMembership() {
      FieldFilterCapability cap = requireField("salary");
      assertThat(cap.getFieldType()).isEqualTo("double");
      assertThat(cap.getAllowedOperatorsList())
          .contains(FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN_OR_EQUAL);
      // IN/NOT_IN not in salary's allowlist (no string representation contract for doubles)
      assertThat(cap.getAllowedOperatorsList())
          .doesNotContain(FilterOperator.IN, FilterOperator.NOT_IN);
    }
  }

  @Nested
  class BooleanFields {

    @Test
    void active_onlyAllowsEqualityOperators() {
      FieldFilterCapability cap = requireField("active");
      assertThat(cap.getFieldType()).isEqualTo("bool");
      assertThat(cap.getAllowedOperatorsList())
          .containsExactlyInAnyOrder(FilterOperator.EQUALS, FilterOperator.NOT_EQUALS);
    }

    @Test
    void active_doesNotAllowAnyOtherOperator() {
      FieldFilterCapability cap = requireField("active");
      assertThat(cap.getAllowedOperatorsList())
          .doesNotContain(
              FilterOperator.GREATER_THAN, FilterOperator.CONTAINS,
              FilterOperator.STARTS_WITH, FilterOperator.IN);
    }
  }

  @Nested
  class NestedFields {

    @Test
    void addressCity_isPresent() {
      FieldFilterCapability cap = requireField("address.city");
      assertThat(cap.getFieldType()).isEqualTo("string");
      assertThat(cap.getAllowedOperatorsList()).contains(FilterOperator.EQUALS, FilterOperator.IN);
    }

    @Test
    void contactEmail_allowsSubstringButNotMembership() {
      FieldFilterCapability cap = requireField("contact.email");
      assertThat(cap.getAllowedOperatorsList())
          .contains(FilterOperator.CONTAINS, FilterOperator.STARTS_WITH, FilterOperator.ENDS_WITH);
      assertThat(cap.getAllowedOperatorsList())
          .doesNotContain(FilterOperator.IN, FilterOperator.NOT_IN);
    }
  }

  @Nested
  class UnknownFields {

    @Test
    void unknownField_returnsEmpty() {
      Optional<FieldFilterCapability> result = registry.find("nonExistent");
      assertThat(result).isEmpty();
    }

    @Test
    void partialPath_returnsEmpty() {
      // "address" alone is a nested message, not a scalar field — not directly filterable
      Optional<FieldFilterCapability> result = registry.find("address");
      assertThat(result).isEmpty();
    }
  }

  private FieldFilterCapability requireField(String path) {
    return registry
        .find(path)
        .orElseThrow(() -> new AssertionError("Expected field '" + path + "' in registry"));
  }
}
