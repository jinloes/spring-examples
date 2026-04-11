package com.jinloes.grpc.filter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jinloes.grpc.filter.proto.FieldFilter;
import com.jinloes.grpc.filter.proto.FilterOperator;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.LogicalOperator;
import com.jinloes.grpc.filter.proto.Person;
import com.jinloes.grpc.filter.proto.PersonFilterServiceGrpc;
import com.jinloes.grpc.filter.proto.PersonsResponse;
import com.jinloes.grpc.filter.proto.StringList;
import io.grpc.StatusRuntimeException;
import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for {@link PersonFilterServiceImpl} running on an in-process gRPC server.
 * Exercises the full gRPC stack (serialization → service → filter engine) without network I/O.
 */
@SpringBootTest
@ActiveProfiles("test")
class PersonFilterServiceImplTest {

  @GrpcClient("inProcess")
  private PersonFilterServiceGrpc.PersonFilterServiceBlockingStub stub;

  // ---- Baseline ----

  @Test
  void noFilters_returnsAllEightPersons() {
    PersonsResponse response = stub.listPersons(FilterRequest.newBuilder().build());
    assertThat(response.getTotalMatched()).isEqualTo(8);
    assertThat(response.getPersonsList()).hasSize(8);
  }

  // ---- Top-level field filters ----

  @Nested
  class TopLevelFilters {

    @Test
    void filterByDepartment_equals() {
      PersonsResponse response =
          stub.listPersons(request(filter("department", FilterOperator.EQUALS, "Engineering")));
      assertThat(response.getTotalMatched()).isEqualTo(3);
      assertThat(names(response))
          .containsExactlyInAnyOrder("Alice Johnson", "Carol Williams", "Eve Davis");
    }

    @Test
    void filterByAge_greaterThan() {
      PersonsResponse response =
          stub.listPersons(request(filterInt("age", FilterOperator.GREATER_THAN, 40)));
      assertThat(response.getPersonsList()).allMatch(p -> p.getAge() > 40);
      assertNames(response, "Bob Smith", "David Brown", "Frank Miller");
    }

    @Test
    void filterByActive_false() {
      PersonsResponse response =
          stub.listPersons(request(filterBool("active", FilterOperator.EQUALS, false)));
      assertThat(response.getPersonsList()).noneMatch(Person::getActive);
      assertNames(response, "Carol Williams", "Frank Miller");
    }

    @Test
    void filterBySalary_lessThan() {
      PersonsResponse response =
          stub.listPersons(request(filterDouble("salary", FilterOperator.LESS_THAN, 80000.0)));
      assertThat(response.getPersonsList()).allMatch(p -> p.getSalary() < 80000.0);
      assertNames(response, "Bob Smith", "Grace Wilson", "Henry Moore");
    }

    @Test
    void filterByName_contains() {
      // "son" appears in "Johnson" and "Wilson"
      PersonsResponse response =
          stub.listPersons(request(filter("name", FilterOperator.CONTAINS, "son")));
      assertNames(response, "Alice Johnson", "Grace Wilson");
    }

    @Test
    void filterByDepartment_in() {
      PersonsResponse response = stub.listPersons(request(filterIn("department", "Finance", "HR")));
      assertThat(response.getTotalMatched()).isEqualTo(3); // David, Grace, Henry
      assertNames(response, "David Brown", "Grace Wilson", "Henry Moore");
    }
  }

  // ---- Nested field filters ----

  @Nested
  class NestedFieldFilters {

    @Test
    void filterByAddressCity_equals() {
      PersonsResponse response =
          stub.listPersons(request(filter("address.city", FilterOperator.EQUALS, "Austin")));
      assertNames(response, "Eve Davis", "Henry Moore");
    }

    @Test
    void filterByAddressState_equals() {
      PersonsResponse response =
          stub.listPersons(request(filter("address.state", FilterOperator.EQUALS, "NY")));
      assertNames(response, "Bob Smith", "Frank Miller");
    }

    @Test
    void filterByContactEmail_startsWith() {
      PersonsResponse response =
          stub.listPersons(request(filter("contact.email", FilterOperator.STARTS_WITH, "david")));
      assertNames(response, "David Brown");
    }

    @Test
    void filterByAddressCity_in() {
      PersonsResponse response =
          stub.listPersons(request(filterIn("address.city", "Chicago", "Seattle")));
      assertNames(response, "David Brown", "Grace Wilson");
    }
  }

  // ---- Logical AND / OR ----

  @Nested
  class LogicalOperators {

    @Test
    void and_engineeringInSanFrancisco() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("department", FilterOperator.EQUALS, "Engineering"))
              .addFilters(filter("address.city", FilterOperator.EQUALS, "San Francisco"))
              .setLogicalOperator(LogicalOperator.AND)
              .build();
      PersonsResponse response = stub.listPersons(req);
      assertNames(response, "Alice Johnson", "Carol Williams");
    }

    @Test
    void and_activeEngineerWithHighSalary() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filterBool("active", FilterOperator.EQUALS, true))
              .addFilters(filter("department", FilterOperator.EQUALS, "Engineering"))
              .addFilters(filterDouble("salary", FilterOperator.GREATER_THAN, 90000.0))
              .setLogicalOperator(LogicalOperator.AND)
              .build();
      PersonsResponse response = stub.listPersons(req);
      assertNames(response, "Alice Johnson", "Eve Davis");
    }

    @Test
    void or_sfOrAustin() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("address.city", FilterOperator.EQUALS, "San Francisco"))
              .addFilters(filter("address.city", FilterOperator.EQUALS, "Austin"))
              .setLogicalOperator(LogicalOperator.OR)
              .build();
      PersonsResponse response = stub.listPersons(req);
      assertThat(response.getTotalMatched()).isEqualTo(4);
      assertNames(response, "Alice Johnson", "Carol Williams", "Eve Davis", "Henry Moore");
    }

    @Test
    void or_ageUnder30OrOver50() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filterInt("age", FilterOperator.LESS_THAN, 30))
              .addFilters(filterInt("age", FilterOperator.GREATER_THAN, 50))
              .setLogicalOperator(LogicalOperator.OR)
              .build();
      PersonsResponse response = stub.listPersons(req);
      assertThat(response.getPersonsList()).allMatch(p -> p.getAge() < 30 || p.getAge() > 50);
      assertNames(response, "Carol Williams", "Grace Wilson", "David Brown");
    }
  }

  // ---- Error handling ----

  @Nested
  class ErrorHandling {

    @Test
    void unknownField_returnsInvalidArgument() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("nonExistentField", FilterOperator.EQUALS, "x"))
              .build();
      assertThatThrownBy(() -> stub.listPersons(req))
          .isInstanceOf(StatusRuntimeException.class)
          .hasMessageContaining("INVALID_ARGUMENT");
    }

    @Test
    void traversingScalarAsMessage_returnsInvalidArgument() {
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("name.first", FilterOperator.EQUALS, "Alice"))
              .build();
      assertThatThrownBy(() -> stub.listPersons(req))
          .isInstanceOf(StatusRuntimeException.class)
          .hasMessageContaining("INVALID_ARGUMENT");
    }

    // ---- Operator allowlist violations (caught by FilterRequestValidator) ----

    @Test
    void contains_onBoolField_returnsInvalidArgument() {
      // active is bool — CONTAINS is not in its allowlist
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filter("active", FilterOperator.CONTAINS, "true"))
              .build();
      assertThatThrownBy(() -> stub.listPersons(req))
          .isInstanceOf(StatusRuntimeException.class)
          .hasMessageContaining("INVALID_ARGUMENT");
    }

    @Test
    void greaterThan_onStringField_returnsInvalidArgument() {
      // name is a string — GREATER_THAN is not in its allowlist
      FilterRequest req =
          FilterRequest.newBuilder()
              .addFilters(filterInt("name", FilterOperator.GREATER_THAN, 5))
              .build();
      assertThatThrownBy(() -> stub.listPersons(req))
          .isInstanceOf(StatusRuntimeException.class)
          .hasMessageContaining("INVALID_ARGUMENT");
    }

    @Test
    void in_onSalaryField_returnsInvalidArgument() {
      // salary's allowlist excludes IN
      FilterRequest req =
          FilterRequest.newBuilder().addFilters(filterIn("salary", "95000.0")).build();
      assertThatThrownBy(() -> stub.listPersons(req))
          .isInstanceOf(StatusRuntimeException.class)
          .hasMessageContaining("INVALID_ARGUMENT");
    }
  }

  // ---- Helper builders ----

  private FilterRequest request(FieldFilter fieldFilter) {
    return FilterRequest.newBuilder().addFilters(fieldFilter).build();
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

  private FieldFilter filterDouble(String path, FilterOperator op, double value) {
    return FieldFilter.newBuilder()
        .setFieldPath(path)
        .setOperator(op)
        .setDoubleValue(value)
        .build();
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

  private List<String> names(PersonsResponse response) {
    return response.getPersonsList().stream().map(Person::getName).toList();
  }

  private void assertNames(PersonsResponse response, String... expectedNames) {
    assertThat(names(response)).containsExactlyInAnyOrder(expectedNames);
  }
}
