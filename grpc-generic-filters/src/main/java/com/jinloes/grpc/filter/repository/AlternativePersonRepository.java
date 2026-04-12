package com.jinloes.grpc.filter.repository;

import static com.jinloes.grpc.filter.engine.FieldExtractorBuilder.path;

import com.jinloes.grpc.filter.engine.FieldExtractorBuilder;
import com.jinloes.grpc.filter.engine.FilterEngine;
import com.jinloes.grpc.filter.proto.Address;
import com.jinloes.grpc.filter.proto.ContactInfo;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.Person;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Alternative backend whose internal model uses different field names than the proto / {@link
 * com.jinloes.grpc.filter.repository.model.PersonModel}. Owns a dedicated {@link FilterEngine} that
 * filters at the {@link PersonRecord} level.
 *
 * <p>The {@link FieldExtractorBuilder} binds each proto field path to the corresponding accessor on
 * the internal model. The proto descriptor validates every path at startup, so a typo or stale
 * mapping fails immediately rather than at query time.
 *
 * <p>Proto field path → internal field:
 *
 * <ul>
 *   <li>{@code id} → {@code personId}
 *   <li>{@code name} → {@code fullName}
 *   <li>{@code age} → {@code yearsOld}
 *   <li>{@code department} → {@code team}
 *   <li>{@code salary} → {@code annualSalary}
 *   <li>{@code active} → {@code employed}
 *   <li>{@code address.street} → {@code location.streetAddress}
 *   <li>{@code address.city} → {@code location.cityName}
 *   <li>{@code address.state} → {@code location.stateName}
 *   <li>{@code address.zip} → {@code location.postalCode}
 *   <li>{@code address.country} → {@code location.countryName}
 *   <li>{@code contact.email} → {@code contactDetails.emailAddress}
 *   <li>{@code contact.phone} → {@code contactDetails.phoneNumber}
 * </ul>
 */
@Repository("alternative")
public class AlternativePersonRepository implements PersonRepository {

  record LocationRecord(
      String streetAddress,
      String cityName,
      String stateName,
      String postalCode,
      String countryName) {}

  record ContactRecord(String emailAddress, String phoneNumber) {}

  record PersonRecord(
      String personId,
      String fullName,
      int yearsOld,
      String team,
      double annualSalary,
      boolean employed,
      LocationRecord location,
      ContactRecord contactDetails,
      List<String> labels) {}

  private static final List<PersonRecord> RECORDS =
      List.of(
          new PersonRecord(
              "1",
              "Alice Johnson",
              30,
              "Engineering",
              95000.0,
              true,
              new LocationRecord("123 Main St", "San Francisco", "CA", "94102", "US"),
              new ContactRecord("alice@example.com", "415-555-0101"),
              List.of("java", "grpc")),
          new PersonRecord(
              "2",
              "Bob Smith",
              45,
              "Marketing",
              75000.0,
              true,
              new LocationRecord("456 Broadway", "New York", "NY", "10013", "US"),
              new ContactRecord("bob@example.com", "212-555-0202"),
              List.of("marketing", "analytics")),
          new PersonRecord(
              "3",
              "Carol Williams",
              28,
              "Engineering",
              88000.0,
              false,
              new LocationRecord("789 Market St", "San Francisco", "CA", "94103", "US"),
              new ContactRecord("carol@example.com", "415-555-0303"),
              List.of("java", "spring")),
          new PersonRecord(
              "4",
              "David Brown",
              52,
              "Finance",
              120000.0,
              true,
              new LocationRecord("101 Lake Shore Dr", "Chicago", "IL", "60601", "US"),
              new ContactRecord("david@example.com", "312-555-0404"),
              List.of("finance", "excel")),
          new PersonRecord(
              "5",
              "Eve Davis",
              35,
              "Engineering",
              105000.0,
              true,
              new LocationRecord("202 Congress Ave", "Austin", "TX", "78701", "US"),
              new ContactRecord("eve@example.com", "512-555-0505"),
              List.of("java", "kubernetes")),
          new PersonRecord(
              "6",
              "Frank Miller",
              41,
              "Marketing",
              82000.0,
              false,
              new LocationRecord("303 5th Ave", "New York", "NY", "10016", "US"),
              new ContactRecord("frank@example.com", "212-555-0606"),
              List.of("marketing", "seo")),
          new PersonRecord(
              "7",
              "Grace Wilson",
              29,
              "Finance",
              71000.0,
              true,
              new LocationRecord("404 Pine St", "Seattle", "WA", "98101", "US"),
              new ContactRecord("grace@example.com", "206-555-0707"),
              List.of("finance", "accounting")),
          new PersonRecord(
              "8",
              "Henry Moore",
              38,
              "HR",
              65000.0,
              true,
              new LocationRecord("505 6th St", "Austin", "TX", "78702", "US"),
              new ContactRecord("henry@example.com", "512-555-0808"),
              List.of("hr", "recruiting")));

  private final FilterEngine<PersonRecord> filterEngine =
      new FilterEngine<>(
          FieldExtractorBuilder.<PersonRecord>forProto(Person.getDescriptor())
              .bind(Person.ID_FIELD_NUMBER, PersonRecord::personId)
              .bind(Person.NAME_FIELD_NUMBER, PersonRecord::fullName)
              .bind(Person.AGE_FIELD_NUMBER, r -> r.yearsOld())
              .bind(Person.DEPARTMENT_FIELD_NUMBER, PersonRecord::team)
              .bind(Person.SALARY_FIELD_NUMBER, r -> r.annualSalary())
              .bind(Person.ACTIVE_FIELD_NUMBER, r -> r.employed())
              .bind(
                  path(Person.ADDRESS_FIELD_NUMBER, Address.STREET_FIELD_NUMBER),
                  r -> r.location().streetAddress())
              .bind(
                  path(Person.ADDRESS_FIELD_NUMBER, Address.CITY_FIELD_NUMBER),
                  r -> r.location().cityName())
              .bind(
                  path(Person.ADDRESS_FIELD_NUMBER, Address.STATE_FIELD_NUMBER),
                  r -> r.location().stateName())
              .bind(
                  path(Person.ADDRESS_FIELD_NUMBER, Address.ZIP_FIELD_NUMBER),
                  r -> r.location().postalCode())
              .bind(
                  path(Person.ADDRESS_FIELD_NUMBER, Address.COUNTRY_FIELD_NUMBER),
                  r -> r.location().countryName())
              .bind(
                  path(Person.CONTACT_FIELD_NUMBER, ContactInfo.EMAIL_FIELD_NUMBER),
                  r -> r.contactDetails().emailAddress())
              .bind(
                  path(Person.CONTACT_FIELD_NUMBER, ContactInfo.PHONE_FIELD_NUMBER),
                  r -> r.contactDetails().phoneNumber())
              .build());

  @Override
  public List<Person> filter(FilterRequest request) {
    return filterEngine.apply(RECORDS, request).stream()
        .map(AlternativePersonRepository::toProto)
        .toList();
  }

  private static Person toProto(PersonRecord r) {
    return Person.newBuilder()
        .setId(r.personId())
        .setName(r.fullName())
        .setAge(r.yearsOld())
        .setDepartment(r.team())
        .setSalary(r.annualSalary())
        .setActive(r.employed())
        .setAddress(
            Address.newBuilder()
                .setStreet(r.location().streetAddress())
                .setCity(r.location().cityName())
                .setState(r.location().stateName())
                .setZip(r.location().postalCode())
                .setCountry(r.location().countryName())
                .build())
        .setContact(
            ContactInfo.newBuilder()
                .setEmail(r.contactDetails().emailAddress())
                .setPhone(r.contactDetails().phoneNumber())
                .build())
        .addAllTags(r.labels())
        .build();
  }
}
