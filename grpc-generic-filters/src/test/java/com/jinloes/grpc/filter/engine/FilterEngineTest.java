package com.jinloes.grpc.filter.engine;

import static com.jinloes.grpc.filter.engine.FieldExtractorBuilder.path;
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
import com.jinloes.grpc.filter.repository.model.AddressModel;
import com.jinloes.grpc.filter.repository.model.ContactModel;
import com.jinloes.grpc.filter.repository.model.PersonModel;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FilterEngine}. Uses {@link PersonModel} as the record type and the same
 * {@link FieldExtractor} as {@link InMemoryPersonRepository}, testing all operators, nested field
 * traversal, and the AND / OR logical operators.
 */
class FilterEngineTest {

  private FilterEngine<PersonModel> filterEngine;
  private List<PersonModel> persons;

  @BeforeEach
  void setUp() {
    filterEngine =
        new FilterEngine<>(
            FieldExtractorBuilder.<PersonModel>forProto(Person.getDescriptor())
                .bind(Person.ID_FIELD_NUMBER, PersonModel::id)
                .bind(Person.NAME_FIELD_NUMBER, PersonModel::name)
                .bind(Person.AGE_FIELD_NUMBER, m -> m.age())
                .bind(Person.DEPARTMENT_FIELD_NUMBER, PersonModel::department)
                .bind(Person.SALARY_FIELD_NUMBER, m -> m.salary())
                .bind(Person.ACTIVE_FIELD_NUMBER, m -> m.active())
                .bind(
                    path(Person.ADDRESS_FIELD_NUMBER, Address.STREET_FIELD_NUMBER),
                    m -> m.address().street())
                .bind(
                    path(Person.ADDRESS_FIELD_NUMBER, Address.CITY_FIELD_NUMBER),
                    m -> m.address().city())
                .bind(
                    path(Person.ADDRESS_FIELD_NUMBER, Address.STATE_FIELD_NUMBER),
                    m -> m.address().state())
                .bind(
                    path(Person.ADDRESS_FIELD_NUMBER, Address.ZIP_FIELD_NUMBER),
                    m -> m.address().zip())
                .bind(
                    path(Person.ADDRESS_FIELD_NUMBER, Address.COUNTRY_FIELD_NUMBER),
                    m -> m.address().country())
                .bind(
                    path(Person.CONTACT_FIELD_NUMBER, ContactInfo.EMAIL_FIELD_NUMBER),
                    m -> m.contact().email())
                .bind(
                    path(Person.CONTACT_FIELD_NUMBER, ContactInfo.PHONE_FIELD_NUMBER),
                    m -> m.contact().phone())
                .build());
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
      List<PersonModel> result = apply(filter("name", FilterOperator.EQUALS, "Alice Johnson"));
      assertNames(result, "Alice Johnson");
    }

    @Test
    void notEquals_excludesMatchedName() {
      List<PersonModel> result = apply(filter("name", FilterOperator.NOT_EQUALS, "Alice Johnson"));
      assertThat(result).noneMatch(p -> p.name().equals("Alice Johnson"));
      assertThat(result).hasSize(persons.size() - 1);
    }

    @Test
    void contains_caseInsensitive() {
      List<PersonModel> result = apply(filter("name", FilterOperator.CONTAINS, "miller"));
      assertNames(result, "Frank Miller");
    }

    @Test
    void startsWith_caseInsensitive() {
      List<PersonModel> result = apply(filter("name", FilterOperator.STARTS_WITH, "eve"));
      assertNames(result, "Eve Davis");
    }

    @Test
    void endsWith_caseInsensitive() {
      List<PersonModel> result = apply(filter("name", FilterOperator.ENDS_WITH, "BROWN"));
      assertNames(result, "David Brown");
    }

    @Test
    void in_matchesMultipleNames() {
      List<PersonModel> result =
          apply(filterIn("name", "Alice Johnson", "Grace Wilson", "Henry Moore"));
      assertNames(result, "Alice Johnson", "Grace Wilson", "Henry Moore");
    }

    @Test
    void notIn_excludesListedDepartments() {
      List<PersonModel> result = apply(filterIn_not("department", "Engineering", "Marketing"));
      assertThat(result)
          .allMatch(p -> !List.of("Engineering", "Marketing").contains(p.department()));
    }
  }

  // ---- Top-level integer fields ----

  @Nested
  class TopLevelIntegerFields {

    @Test
    void equals_byAge() {
      List<PersonModel> result = apply(filterInt("age", FilterOperator.EQUALS, 30));
      assertNames(result, "Alice Johnson");
    }

    @Test
    void greaterThan_byAge() {
      List<PersonModel> result = apply(filterInt("age", FilterOperator.GREATER_THAN, 40));
      assertThat(result).allMatch(p -> p.age() > 40);
      assertNames(result, "Bob Smith", "David Brown", "Frank Miller");
    }

    @Test
    void lessThan_byAge() {
      List<PersonModel> result = apply(filterInt("age", FilterOperator.LESS_THAN, 30));
      assertThat(result).allMatch(p -> p.age() < 30);
      assertNames(result, "Carol Williams", "Grace Wilson");
    }

    @Test
    void greaterThanOrEqual_byAge() {
      List<PersonModel> result = apply(filterInt("age", FilterOperator.GREATER_THAN_OR_EQUAL, 45));
      assertThat(result).allMatch(p -> p.age() >= 45);
      assertNames(result, "Bob Smith", "David Brown");
    }

    @Test
    void lessThanOrEqual_byAge() {
      List<PersonModel> result = apply(filterInt("age", FilterOperator.LESS_THAN_OR_EQUAL, 29));
      assertThat(result).allMatch(p -> p.age() <= 29);
      assertNames(result, "Carol Williams", "Grace Wilson");
    }

    @Test
    void in_byAge_usingStringRepresentation() {
      List<PersonModel> result = apply(filterIn("age", "30", "52"));
      assertNames(result, "Alice Johnson", "David Brown");
    }
  }

  // ---- Top-level double fields ----

  @Nested
  class TopLevelDoubleFields {

    @Test
    void greaterThan_bySalary() {
      List<PersonModel> result =
          apply(filterDouble("salary", FilterOperator.GREATER_THAN, 100000.0));
      assertThat(result).allMatch(p -> p.salary() > 100000.0);
      assertNames(result, "Eve Davis", "David Brown");
    }

    @Test
    void lessThanOrEqual_bySalary() {
      List<PersonModel> result =
          apply(filterDouble("salary", FilterOperator.LESS_THAN_OR_EQUAL, 75000.0));
      assertThat(result).allMatch(p -> p.salary() <= 75000.0);
      assertNames(result, "Bob Smith", "Grace Wilson", "Henry Moore");
    }
  }

  // ---- Top-level boolean fields ----

  @Nested
  class TopLevelBooleanFields {

    @Test
    void equals_activeTrue() {
      List<PersonModel> result = apply(filterBool("active", FilterOperator.EQUALS, true));
      assertThat(result).allMatch(PersonModel::active);
      assertThat(result).hasSize(6);
    }

    @Test
    void equals_activeFalse() {
      List<PersonModel> result = apply(filterBool("active", FilterOperator.EQUALS, false));
      assertThat(result).noneMatch(PersonModel::active);
      assertNames(result, "Carol Williams", "Frank Miller");
    }
  }

  // ---- Nested fields (dot-notation) ----

  @Nested
  class NestedFields {

    @Test
    void equals_nestedAddressCity() {
      List<PersonModel> result =
          apply(filter("address.city", FilterOperator.EQUALS, "San Francisco"));
      assertNames(result, "Alice Johnson", "Carol Williams");
    }

    @Test
    void equals_nestedAddressState() {
      List<PersonModel> result = apply(filter("address.state", FilterOperator.EQUALS, "TX"));
      assertNames(result, "Eve Davis", "Henry Moore");
    }

    @Test
    void equals_nestedAddressCountry() {
      List<PersonModel> result = apply(filter("address.country", FilterOperator.EQUALS, "US"));
      assertThat(result).hasSize(persons.size()); // all are US
    }

    @Test
    void contains_nestedAddressCity() {
      List<PersonModel> result = apply(filter("address.city", FilterOperator.CONTAINS, "new"));
      assertNames(result, "Bob Smith", "Frank Miller");
    }

    @Test
    void in_nestedAddressCity() {
      List<PersonModel> result = apply(filterIn("address.city", "Austin", "Seattle"));
      assertNames(result, "Eve Davis", "Henry Moore", "Grace Wilson");
    }

    @Test
    void equals_nestedContactEmail() {
      List<PersonModel> result =
          apply(filter("contact.email", FilterOperator.EQUALS, "grace@example.com"));
      assertNames(result, "Grace Wilson");
    }

    @Test
    void contains_nestedContactEmail() {
      List<PersonModel> result =
          apply(filter("contact.email", FilterOperator.CONTAINS, "@example.com"));
      assertThat(result).hasSize(persons.size());
    }

    @Test
    void startsWith_nestedContactPhone() {
      List<PersonModel> result = apply(filter("contact.phone", FilterOperator.STARTS_WITH, "512"));
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
      List<PersonModel> result = filterEngine.apply(persons, request);
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
      List<PersonModel> result = filterEngine.apply(persons, request);
      assertThat(result).allMatch(p -> p.active() && p.salary() > 90000.0);
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
      List<PersonModel> result = filterEngine.apply(persons, request);
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
      List<PersonModel> result = filterEngine.apply(persons, request);
      assertThat(result)
          .allMatch(p -> p.department().equals("Engineering") || p.department().equals("Finance"));
      assertThat(result).hasSize(5); // 3 Engineering + 2 Finance
    }
  }

  // ---- No-match cases ----

  @Nested
  class NoMatches {

    @Test
    void returns_emptyList_whenNoPersonsMatch() {
      List<PersonModel> result =
          apply(filter("department", FilterOperator.EQUALS, "NonExistentDept"));
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
    void unknownNestedField_throwsIllegalArgument() {
      FilterRequest request =
          FilterRequest.newBuilder()
              .addFilters(filter("name.first", FilterOperator.EQUALS, "Alice"))
              .build();
      assertThatThrownBy(() -> filterEngine.apply(persons, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("name.first");
    }
  }

  // ---- Helper builders ----

  private List<PersonModel> apply(FieldFilter fieldFilter) {
    return filterEngine.apply(persons, FilterRequest.newBuilder().addFilters(fieldFilter).build());
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

  private void assertNames(List<PersonModel> result, String... expectedNames) {
    assertThat(result).extracting(PersonModel::name).containsExactlyInAnyOrder(expectedNames);
  }

  // ---- Test data ----

  private List<PersonModel> buildTestPersons() {
    return List.of(
        new PersonModel(
            "1",
            "Alice Johnson",
            30,
            "Engineering",
            95000.0,
            true,
            new AddressModel("123 Main St", "San Francisco", "CA", "94102", "US"),
            new ContactModel("alice@example.com", "415-555-0101"),
            List.of("java", "grpc")),
        new PersonModel(
            "2",
            "Bob Smith",
            45,
            "Marketing",
            75000.0,
            true,
            new AddressModel("456 Broadway", "New York", "NY", "10013", "US"),
            new ContactModel("bob@example.com", "212-555-0202"),
            List.of("marketing", "analytics")),
        new PersonModel(
            "3",
            "Carol Williams",
            28,
            "Engineering",
            88000.0,
            false,
            new AddressModel("789 Market St", "San Francisco", "CA", "94103", "US"),
            new ContactModel("carol@example.com", "415-555-0303"),
            List.of("java", "spring")),
        new PersonModel(
            "4",
            "David Brown",
            52,
            "Finance",
            120000.0,
            true,
            new AddressModel("101 Lake Shore Dr", "Chicago", "IL", "60601", "US"),
            new ContactModel("david@example.com", "312-555-0404"),
            List.of("finance", "excel")),
        new PersonModel(
            "5",
            "Eve Davis",
            35,
            "Engineering",
            105000.0,
            true,
            new AddressModel("202 Congress Ave", "Austin", "TX", "78701", "US"),
            new ContactModel("eve@example.com", "512-555-0505"),
            List.of("java", "kubernetes")),
        new PersonModel(
            "6",
            "Frank Miller",
            41,
            "Marketing",
            82000.0,
            false,
            new AddressModel("303 5th Ave", "New York", "NY", "10016", "US"),
            new ContactModel("frank@example.com", "212-555-0606"),
            List.of("marketing", "seo")),
        new PersonModel(
            "7",
            "Grace Wilson",
            29,
            "Finance",
            71000.0,
            true,
            new AddressModel("404 Pine St", "Seattle", "WA", "98101", "US"),
            new ContactModel("grace@example.com", "206-555-0707"),
            List.of("finance", "accounting")),
        new PersonModel(
            "8",
            "Henry Moore",
            38,
            "HR",
            65000.0,
            true,
            new AddressModel("505 6th St", "Austin", "TX", "78702", "US"),
            new ContactModel("henry@example.com", "512-555-0808"),
            List.of("hr", "recruiting")));
  }
}
