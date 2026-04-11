package com.jinloes.grpc.filter.repository;

import com.jinloes.grpc.filter.proto.Address;
import com.jinloes.grpc.filter.proto.ContactInfo;
import com.jinloes.grpc.filter.proto.Person;
import java.util.List;
import org.springframework.stereotype.Repository;

/** In-memory store of example {@link Person} records used to demonstrate generic filtering. */
@Repository
public class PersonRepository {

  private static final List<Person> PERSONS =
      List.of(
          person(
              "1",
              "Alice Johnson",
              30,
              "Engineering",
              95000.0,
              true,
              address("123 Main St", "San Francisco", "CA", "94102", "US"),
              contact("alice@example.com", "415-555-0101"),
              "java",
              "grpc"),
          person(
              "2",
              "Bob Smith",
              45,
              "Marketing",
              75000.0,
              true,
              address("456 Broadway", "New York", "NY", "10013", "US"),
              contact("bob@example.com", "212-555-0202"),
              "marketing",
              "analytics"),
          person(
              "3",
              "Carol Williams",
              28,
              "Engineering",
              88000.0,
              false,
              address("789 Market St", "San Francisco", "CA", "94103", "US"),
              contact("carol@example.com", "415-555-0303"),
              "java",
              "spring"),
          person(
              "4",
              "David Brown",
              52,
              "Finance",
              120000.0,
              true,
              address("101 Lake Shore Dr", "Chicago", "IL", "60601", "US"),
              contact("david@example.com", "312-555-0404"),
              "finance",
              "excel"),
          person(
              "5",
              "Eve Davis",
              35,
              "Engineering",
              105000.0,
              true,
              address("202 Congress Ave", "Austin", "TX", "78701", "US"),
              contact("eve@example.com", "512-555-0505"),
              "java",
              "kubernetes"),
          person(
              "6",
              "Frank Miller",
              41,
              "Marketing",
              82000.0,
              false,
              address("303 5th Ave", "New York", "NY", "10016", "US"),
              contact("frank@example.com", "212-555-0606"),
              "marketing",
              "seo"),
          person(
              "7",
              "Grace Wilson",
              29,
              "Finance",
              71000.0,
              true,
              address("404 Pine St", "Seattle", "WA", "98101", "US"),
              contact("grace@example.com", "206-555-0707"),
              "finance",
              "accounting"),
          person(
              "8",
              "Henry Moore",
              38,
              "HR",
              65000.0,
              true,
              address("505 6th St", "Austin", "TX", "78702", "US"),
              contact("henry@example.com", "512-555-0808"),
              "hr",
              "recruiting"));

  public List<Person> findAll() {
    return PERSONS;
  }

  private static Person person(
      String id,
      String name,
      int age,
      String department,
      double salary,
      boolean active,
      Address address,
      ContactInfo contact,
      String... tags) {
    return Person.newBuilder()
        .setId(id)
        .setName(name)
        .setAge(age)
        .setDepartment(department)
        .setSalary(salary)
        .setActive(active)
        .setAddress(address)
        .setContact(contact)
        .addAllTags(List.of(tags))
        .build();
  }

  private static Address address(
      String street, String city, String state, String zip, String country) {
    return Address.newBuilder()
        .setStreet(street)
        .setCity(city)
        .setState(state)
        .setZip(zip)
        .setCountry(country)
        .build();
  }

  private static ContactInfo contact(String email, String phone) {
    return ContactInfo.newBuilder().setEmail(email).setPhone(phone).build();
  }
}
