package com.jinloes.grpc.filter.repository;

import static com.jinloes.grpc.filter.engine.FieldExtractorBuilder.path;

import com.jinloes.grpc.filter.engine.FieldExtractor;
import com.jinloes.grpc.filter.engine.FieldExtractorBuilder;
import com.jinloes.grpc.filter.engine.FilterEngine;
import com.jinloes.grpc.filter.proto.Address;
import com.jinloes.grpc.filter.proto.ContactInfo;
import com.jinloes.grpc.filter.proto.FilterRequest;
import com.jinloes.grpc.filter.proto.Person;
import com.jinloes.grpc.filter.repository.model.AddressModel;
import com.jinloes.grpc.filter.repository.model.ContactModel;
import com.jinloes.grpc.filter.repository.model.PersonModel;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * In-memory backend. Stores {@link PersonModel} records (field names aligned with the proto) and
 * owns a dedicated {@link FilterEngine} that filters at the model level before normalizing to
 * {@link Person} proto.
 *
 * <p>Field paths are bound to model accessors via {@link FieldExtractorBuilder}, which validates
 * each path against the {@link Person} proto descriptor at startup.
 */
@Repository("inMemory")
public class InMemoryPersonRepository implements PersonRepository {

  static final FieldExtractor<PersonModel> EXTRACTOR =
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
              path(Person.ADDRESS_FIELD_NUMBER, Address.CITY_FIELD_NUMBER), m -> m.address().city())
          .bind(
              path(Person.ADDRESS_FIELD_NUMBER, Address.STATE_FIELD_NUMBER),
              m -> m.address().state())
          .bind(path(Person.ADDRESS_FIELD_NUMBER, Address.ZIP_FIELD_NUMBER), m -> m.address().zip())
          .bind(
              path(Person.ADDRESS_FIELD_NUMBER, Address.COUNTRY_FIELD_NUMBER),
              m -> m.address().country())
          .bind(
              path(Person.CONTACT_FIELD_NUMBER, ContactInfo.EMAIL_FIELD_NUMBER),
              m -> m.contact().email())
          .bind(
              path(Person.CONTACT_FIELD_NUMBER, ContactInfo.PHONE_FIELD_NUMBER),
              m -> m.contact().phone())
          .build();

  private static final List<PersonModel> PERSONS =
      List.of(
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

  private final FilterEngine<PersonModel> filterEngine = new FilterEngine<>(EXTRACTOR);

  @Override
  public List<Person> filter(FilterRequest request) {
    return filterEngine.apply(PERSONS, request).stream()
        .map(InMemoryPersonRepository::toProto)
        .toList();
  }

  private static Person toProto(PersonModel m) {
    return Person.newBuilder()
        .setId(m.id())
        .setName(m.name())
        .setAge(m.age())
        .setDepartment(m.department())
        .setSalary(m.salary())
        .setActive(m.active())
        .setAddress(
            Address.newBuilder()
                .setStreet(m.address().street())
                .setCity(m.address().city())
                .setState(m.address().state())
                .setZip(m.address().zip())
                .setCountry(m.address().country())
                .build())
        .setContact(
            ContactInfo.newBuilder()
                .setEmail(m.contact().email())
                .setPhone(m.contact().phone())
                .build())
        .addAllTags(m.tags())
        .build();
  }
}
