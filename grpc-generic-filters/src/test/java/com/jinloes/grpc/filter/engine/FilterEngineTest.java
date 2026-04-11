package com.jinloes.grpc.filter.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jinloes.grpc.filter.proto.Address;
import com.jinloes.grpc.filter.proto.ContactInfo;
import com.jinloes.grpc.filter.proto.FieldFilter;
import com.jinloes.grpc.filter.proto.FilterOperator;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.LogicalOperator;
import com.jinloes.grpc.filter.proto.Person;
import com.jinloes.grpc.filter.proto.StringList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FilterEngine}. Tests cover all operators, nested field traversal, and the
 * AND / OR logical operators.
 */
class FilterEngineTest {

  private FilterEngine filterEngine;
  private List<Person> persons;

  @BeforeEach
  void setUp() {
    filterEngine = new FilterEngine();
    persons = buildTestPersons();
  }

  // ---- No filters ----

  @Test
  void noFilters_returnsAllItems() {
    FilterRequest request = FilterRequest.newBuilder().build();
    assertThat(filterEngine.apply(persons, request)).hasSize(persons.size());
  }

  // ---- Top-level string fields ----

  @Nested
  class TopLevelStringFields {

    @Test
    void equals_matchesByExactName() {
      List<Person> result = apply(filter("name", FilterOperator.EQUALS, "Alice Johnson"));
      assertNames(result, "Alice Johnson");
    }

    @Test
    void notEquals_excludesMatchedName() {
      List<Person> result = apply(filter("name", FilterOperator.NOT_EQUALS, "Alice Johnson"));
      assertThat(result).noneMatch(p -> p.getName().equals("Alice Johnson"));
      assertThat(result).hasSize(persons.size() - 1);
    }

    @Test
    void contains_caseInsensitive() {
      List<Person> result = apply(filter("name", FilterOperator.CONTAINS, "miller"));
      assertNames(result, "Frank Miller");
    }

    @Test
    void startsWith_caseInsensitive() {
      List<Person> result = apply(filter("name", FilterOperator.STARTS_WITH, "eve"));
      assertNames(result, "Eve Davis");
    }

    @Test
    void endsWith_caseInsensitive() {
      List<Person> result = apply(filter("name", FilterOperator.ENDS_WITH, "BROWN"));
      assertNames(result, "David Brown");
    }

    @Test
    void in_matchesMultipleNames() {
      List<Person> result = apply(filterIn("name", "Alice Johnson", "Grace Wilson", "Henry Moore"));
      assertNames(result, "Alice Johnson", "Grace Wilson", "Henry Moore");
    }

    @Test
    void notIn_excludesListedNames() {
      List<Person> result = apply(filterIn_not("department", "Engineering", "Marketing"));
      assertThat(result)
          .allMatch(p -> !List.of("Engineering", "Marketing").contains(p.getDepartment()));
    }
  }

  // ---- Top-level integer fields ----

  @Nested
  class TopLevelIntegerFields {

    @Test
    void equals_byAge() {
      List<Person> result = apply(filterInt("age", FilterOperator.EQUALS, 30));
      assertNames(result, "Alice Johnson");
    }

    @Test
    void greaterThan_byAge() {
      List<Person> result = apply(filterInt("age", FilterOperator.GREATER_THAN, 40));
      assertThat(result).allMatch(p -> p.getAge() > 40);
      assertNames(result, "Bob Smith", "David Brown", "Frank Miller");
    }

    @Test
    void lessThan_byAge() {
      List<Person> result = apply(filterInt("age", FilterOperator.LESS_THAN, 30));
      assertThat(result).allMatch(p -> p.getAge() < 30);
      assertNames(result, "Carol Williams", "Grace Wilson");
    }

    @Test
    void greaterThanOrEqual_byAge() {
      List<Person> result = apply(filterInt("age", FilterOperator.GREATER_THAN_OR_EQUAL, 45));
      assertThat(result).allMatch(p -> p.getAge() >= 45);
      assertNames(result, "Bob Smith", "David Brown");
    }

    @Test
    void lessThanOrEqual_byAge() {
      List<Person> result = apply(filterInt("age", FilterOperator.LESS_THAN_OR_EQUAL, 29));
      assertThat(result).allMatch(p -> p.getAge() <= 29);
      assertNames(result, "Carol Williams", "Grace Wilson");
    }

    @Test
    void in_byAge_usingStringRepresentation() {
      // IN uses string_list_value; numeric values are matched via toString
      List<Person> result = apply(filterIn("age", "30", "52"));
      assertNames(result, "Alice Johnson", "David Brown");
    }
  }

  // ---- Top-level double fields ----

  @Nested
  class TopLevelDoubleFields {

    @Test
    void greaterThan_bySalary() {
      List<Person> result = apply(filterDouble("salary", FilterOperator.GREATER_THAN, 100000.0));
      assertThat(result).allMatch(p -> p.getSalary() > 100000.0);
      assertNames(result, "Eve Davis", "David Brown");
    }

    @Test
    void lessThanOrEqual_bySalary() {
      List<Person> result =
          apply(filterDouble("salary", FilterOperator.LESS_THAN_OR_EQUAL, 75000.0));
      assertThat(result).allMatch(p -> p.getSalary() <= 75000.0);
      assertNames(result, "Bob Smith", "Grace Wilson", "Henry Moore");
    }
  }

  // ---- Top-level boolean fields ----

  @Nested
  class TopLevelBooleanFields {

    @Test
    void equals_activeTrue() {
      List<Person> result = apply(filterBool("active", FilterOperator.EQUALS, true));
      assertThat(result).allMatch(Person::getActive);
      assertThat(result).hasSize(6);
    }

    @Test
    void equals_activeFalse() {
      List<Person> result = apply(filterBool("active", FilterOperator.EQUALS, false));
      assertThat(result).noneMatch(Person::getActive);
      assertNames(result, "Carol Williams", "Frank Miller");
    }
  }

  // ---- Nested message fields (dot-notation) ----

  @Nested
  class NestedMessageFields {

    @Test
    void equals_nestedAddressCity() {
      List<Person> result = apply(filter("address.city", FilterOperator.EQUALS, "San Francisco"));
      assertNames(result, "Alice Johnson", "Carol Williams");
    }

    @Test
    void equals_nestedAddressState() {
      List<Person> result = apply(filter("address.state", FilterOperator.EQUALS, "TX"));
      assertNames(result, "Eve Davis", "Henry Moore");
    }

    @Test
    void equals_nestedAddressCountry() {
      List<Person> result = apply(filter("address.country", FilterOperator.EQUALS, "US"));
      assertThat(result).hasSize(persons.size()); // all are US
    }

    @Test
    void contains_nestedAddressCity() {
      List<Person> result = apply(filter("address.city", FilterOperator.CONTAINS, "new"));
      assertNames(result, "Bob Smith", "Frank Miller");
    }

    @Test
    void in_nestedAddressCity() {
      List<Person> result = apply(filterIn("address.city", "Austin", "Seattle"));
      assertNames(result, "Eve Davis", "Henry Moore", "Grace Wilson");
    }

    @Test
    void equals_nestedContactEmail() {
      List<Person> result =
          apply(filter("contact.email", FilterOperator.EQUALS, "grace@example.com"));
      assertNames(result, "Grace Wilson");
    }

    @Test
    void contains_nestedContactEmail() {
      // All emails match "@example.com"
      List<Person> result = apply(filter("contact.email", FilterOperator.CONTAINS, "@example.com"));
      assertThat(result).hasSize(persons.size());
    }

    @Test
    void startsWith_nestedContactPhone() {
      List<Person> result = apply(filter("contact.phone", FilterOperator.STARTS_WITH, "512"));
      assertNames(result, "Eve Davis", "Henry Moore");
    }
  }

  // ---- Logical AND / OR ----

  @Nested
  class LogicalOperators {

    @Test
    void and_cityAndDepartmentMustBothMatch() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("address.city", FilterOperator.EQUALS, "San Francisco"))
              .addFilters(filter("department", FilterOperator.EQUALS, "Engineering"))
              .setLogicalOperator(LogicalOperator.AND)
              .build();
      List<Person> result = filterEngine.apply(persons, request);
      // Only Alice (Engineering, SF) and Carol (Engineering, SF)
      assertNames(result, "Alice Johnson", "Carol Williams");
    }

    @Test
    void and_activeAndHighSalary() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filterBool("active", FilterOperator.EQUALS, true))
              .addFilters(filterDouble("salary", FilterOperator.GREATER_THAN, 90000.0))
              .setLogicalOperator(LogicalOperator.AND)
              .build();
      List<Person> result = filterEngine.apply(persons, request);
      assertThat(result).allMatch(p -> p.getActive() && p.getSalary() > 90000.0);
      assertNames(result, "Alice Johnson", "David Brown", "Eve Davis");
    }

    @Test
    void or_citySanFranciscoOrCityAustin() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("address.city", FilterOperator.EQUALS, "San Francisco"))
              .addFilters(filter("address.city", FilterOperator.EQUALS, "Austin"))
              .setLogicalOperator(LogicalOperator.OR)
              .build();
      List<Person> result = filterEngine.apply(persons, request);
      assertNames(result, "Alice Johnson", "Carol Williams", "Eve Davis", "Henry Moore");
    }

    @Test
    void or_engineeringOrFinance() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("department", FilterOperator.EQUALS, "Engineering"))
              .addFilters(filter("department", FilterOperator.EQUALS, "Finance"))
              .setLogicalOperator(LogicalOperator.OR)
              .build();
      List<Person> result = filterEngine.apply(persons, request);
      assertThat(result)
          .allMatch(
              p -> p.getDepartment().equals("Engineering") || p.getDepartment().equals("Finance"));
      assertThat(result).hasSize(5); // 3 Engineering + 2 Finance
    }
  }

  // ---- No-match cases ----

  @Nested
  class NoMatches {

    @Test
    void returns_emptyList_whenNoPersonsMatch() {
      List<Person> result = apply(filter("department", FilterOperator.EQUALS, "NonExistentDept"));
      assertThat(result).isEmpty();
    }

    @Test
    void and_returns_emptyList_whenNoPersonSatisfiesAllFilters() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("address.city", FilterOperator.EQUALS, "San Francisco"))
              .addFilters(filter("department", FilterOperator.EQUALS, "Finance"))
              .setLogicalOperator(LogicalOperator.AND)
              .build();
      assertThat(filterEngine.apply(persons, request)).isEmpty();
    }
  }

  // ---- Error cases ----

  @Nested
  class ErrorCases {

    @Test
    void unknownTopLevelField_throwsIllegalArgument() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("unknownField", FilterOperator.EQUALS, "x"))
              .build();
      assertThatThrownBy(() -> filterEngine.apply(persons, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("unknownField");
    }

    @Test
    void traversingScalarAsNestedMessage_throwsIllegalArgument() {
      // "name" is a string, not a message — cannot traverse further
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("name.first", FilterOperator.EQUALS, "Alice"))
              .build();
      assertThatThrownBy(() -> filterEngine.apply(persons, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name");
    }
  }

  // ---- Helper builders ----

  private List<Person> apply(FieldFilter fieldFilter) {
    FilterRequest request = FilterRequest.newBuilder().addFilters(fieldFilter).build();
    return filterEngine.apply(persons, request);
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

  private FieldFilter filterIn_not(String path, String... values) {
    return FieldFilter.newBuilder()
        .setFieldPath(path)
        .setOperator(FilterOperator.NOT_IN)
        .setStringListValue(StringList.newBuilder().addAllValues(List.of(values)).build())
        .build();
  }

  private void assertNames(List<Person> result, String... expectedNames) {
    assertThat(result).extracting(Person::getName).containsExactlyInAnyOrder(expectedNames);
  }

  // ---- Test data ----

  private List<Person> buildTestPersons() {
    return List.of(
        person(
            "1",
            "Alice Johnson",
            30,
            "Engineering",
            95000.0,
            true,
            "San Francisco",
            "CA",
            "US",
            "alice@example.com",
            "415-555-0101"),
        person(
            "2",
            "Bob Smith",
            45,
            "Marketing",
            75000.0,
            true,
            "New York",
            "NY",
            "US",
            "bob@example.com",
            "212-555-0202"),
        person(
            "3",
            "Carol Williams",
            28,
            "Engineering",
            88000.0,
            false,
            "San Francisco",
            "CA",
            "US",
            "carol@example.com",
            "415-555-0303"),
        person(
            "4",
            "David Brown",
            52,
            "Finance",
            120000.0,
            true,
            "Chicago",
            "IL",
            "US",
            "david@example.com",
            "312-555-0404"),
        person(
            "5",
            "Eve Davis",
            35,
            "Engineering",
            105000.0,
            true,
            "Austin",
            "TX",
            "US",
            "eve@example.com",
            "512-555-0505"),
        person(
            "6",
            "Frank Miller",
            41,
            "Marketing",
            82000.0,
            false,
            "New York",
            "NY",
            "US",
            "frank@example.com",
            "212-555-0606"),
        person(
            "7",
            "Grace Wilson",
            29,
            "Finance",
            71000.0,
            true,
            "Seattle",
            "WA",
            "US",
            "grace@example.com",
            "206-555-0707"),
        person(
            "8",
            "Henry Moore",
            38,
            "HR",
            65000.0,
            true,
            "Austin",
            "TX",
            "US",
            "henry@example.com",
            "512-555-0808"));
  }

  private Person person(
      String id,
      String name,
      int age,
      String dept,
      double salary,
      boolean active,
      String city,
      String state,
      String country,
      String email,
      String phone) {
    return Person.newBuilder()
        .setId(id)
        .setName(name)
        .setAge(age)
        .setDepartment(dept)
        .setSalary(salary)
        .setActive(active)
        .setAddress(Address.newBuilder().setCity(city).setState(state).setCountry(country).build())
        .setContact(ContactInfo.newBuilder().setEmail(email).setPhone(phone).build())
        .build();
  }
}
