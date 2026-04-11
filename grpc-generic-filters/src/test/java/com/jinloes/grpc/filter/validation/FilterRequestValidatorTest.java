package com.jinloes.grpc.filter.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jinloes.grpc.filter.capability.FilterCapabilityRegistry;
import com.jinloes.grpc.filter.proto.FieldFilter;
import com.jinloes.grpc.filter.proto.FilterOperator;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.LogicalOperator;
import com.jinloes.grpc.filter.proto.StringList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FilterRequestValidatorTest {

  private FilterRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new FilterRequestValidator(new FilterCapabilityRegistry());
  }

  @Test
  void emptyRequest_isAlwaysValid() {
    assertThatCode(() -> validator.validate(FilterRequest.newBuilder().build()))
        .doesNotThrowAnyException();
  }

  @Nested
  class ValidFilters {

    @Test
    void stringEquals_onName_isValid() {
      assertThatCode(
              () -> validator.validate(request(filter("name", FilterOperator.EQUALS, "Alice"))))
          .doesNotThrowAnyException();
    }

    @Test
    void contains_onAddressCity_isValid() {
      assertThatCode(
              () ->
                  validator.validate(
                      request(filter("address.city", FilterOperator.CONTAINS, "York"))))
          .doesNotThrowAnyException();
    }

    @Test
    void greaterThan_onAge_isValid() {
      assertThatCode(
              () -> validator.validate(request(filterInt("age", FilterOperator.GREATER_THAN, 30))))
          .doesNotThrowAnyException();
    }

    @Test
    void equals_onActive_isValid() {
      assertThatCode(
              () -> validator.validate(request(filterBool("active", FilterOperator.EQUALS, true))))
          .doesNotThrowAnyException();
    }

    @Test
    void in_onDepartment_isValid() {
      assertThatCode(
              () -> validator.validate(request(filterIn("department", "Engineering", "Finance"))))
          .doesNotThrowAnyException();
    }

    @Test
    void multipleFilters_allValid_isAccepted() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("name", FilterOperator.CONTAINS, "Ali"))
              .addFilters(filterInt("age", FilterOperator.GREATER_THAN, 25))
              .addFilters(filterBool("active", FilterOperator.EQUALS, true))
              .setLogicalOperator(LogicalOperator.AND)
              .build();
      assertThatCode(() -> validator.validate(req)).doesNotThrowAnyException();
    }
  }

  @Nested
  class UnknownFields {

    @Test
    void unknownTopLevelField_throwsInvalidFilterException() {
      assertThatThrownBy(
              () -> validator.validate(request(filter("nonExistent", FilterOperator.EQUALS, "x"))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("nonExistent")
          .hasMessageContaining("Valid fields:");
    }

    @Test
    void messageFieldWithoutPath_throwsInvalidFilterException() {
      // "address" is a nested message, not a scalar — not directly filterable
      assertThatThrownBy(
              () -> validator.validate(request(filter("address", FilterOperator.EQUALS, "x"))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("address");
    }
  }

  @Nested
  class DisallowedOperators {

    @Test
    void contains_onActive_bool_isRejected() {
      assertThatThrownBy(
              () -> validator.validate(request(filter("active", FilterOperator.CONTAINS, "true"))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("CONTAINS")
          .hasMessageContaining("active")
          .hasMessageContaining("Allowed operators:");
    }

    @Test
    void greaterThan_onName_string_isRejected() {
      assertThatThrownBy(
              () -> validator.validate(request(filterInt("name", FilterOperator.GREATER_THAN, 5))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("GREATER_THAN")
          .hasMessageContaining("name");
    }

    @Test
    void contains_onAge_int_isRejected() {
      assertThatThrownBy(
              () -> validator.validate(request(filter("age", FilterOperator.CONTAINS, "3"))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("CONTAINS")
          .hasMessageContaining("age");
    }

    @Test
    void in_onSalary_isRejected() {
      // salary's allowlist deliberately excludes IN (no string repr contract for doubles)
      assertThatThrownBy(() -> validator.validate(request(filterIn("salary", "95000.0"))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("IN")
          .hasMessageContaining("salary");
    }

    @Test
    void startsWith_onContactPhone_onlyStartsWithIsAllowed() {
      // ENDS_WITH is not in contact.phone's allowlist
      assertThatThrownBy(
              () ->
                  validator.validate(
                      request(filter("contact.phone", FilterOperator.ENDS_WITH, "0101"))))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("ENDS_WITH")
          .hasMessageContaining("contact.phone");
    }

    @Test
    void firstValidFilter_secondInvalid_stillThrows() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("name", FilterOperator.EQUALS, "Alice")) // valid
              .addFilters(filter("active", FilterOperator.GREATER_THAN, "x")) // invalid
              .build();
      assertThatThrownBy(() -> validator.validate(req))
          .isInstanceOf(InvalidFilterException.class)
          .hasMessageContaining("GREATER_THAN")
          .hasMessageContaining("active");
    }
  }

  // ---- Helpers ----

  private FilterRequest request(FieldFilter f) {
    return FilterRequest.newBuilder().addFilters(f).build();
  }

  private FieldFilter filter(String path, FilterOperator op, String value) {
    return FieldFilter.newBuilder()
        .setFieldPath(path)
        .setOperator(op)
        .setStringValue(value)
        .build();
  }

  private FieldFilter filterInt(String path, FilterOperator op, long value) {
    return FieldFilter.newBuilder().setFieldPath(path).setOperator(op).setIntValue(value).build();
  }

  private FieldFilter filterBool(String path, FilterOperator op, boolean value) {
    return FieldFilter.newBuilder().setFieldPath(path).setOperator(op).setBoolValue(value).build();
  }

  private FieldFilter filterIn(String path, String... values) {
    return FieldFilter.newBuilder()
        .setFieldPath(path)
        .setOperator(FilterOperator.IN)
        .setStringListValue(StringList.newBuilder().addAllValues(List.of(values)).build())
        .build();
  }
}
