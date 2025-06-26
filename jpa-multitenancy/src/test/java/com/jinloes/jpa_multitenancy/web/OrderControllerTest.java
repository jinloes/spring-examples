package com.jinloes.jpa_multitenancy.web;


import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.jpa_multitenancy.TenantContext;
import com.jinloes.jpa_multitenancy.data.OrderRepository;
import com.jinloes.jpa_multitenancy.model.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerTest {

  @Container
  private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:13")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgresContainer::getUsername);
    registry.add("spring.datasource.password", postgresContainer::getPassword);
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
  }

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private WebTestClient client;

  private String customerId;
  private String customer2Id;

  @BeforeEach
  void setUp() {
    customerId = "98b67afd-0d75-4c88-8d81-71821688f345";
    customer2Id = "98b67afd-0d75-4c88-8d81-71821688f346";
    TenantContext.setCurrentTenant(customerId);
    orderRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createOrder() {
    Order order = new Order()
        .setName("Order 1")
        .setCustomerId(customerId);

    client.post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("X-Tenant", customerId)
        .body(BodyInserters.fromValue(order))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(Order.class)
        .consumeWith(response -> {
          Order actual = response.getResponseBody();
          assertThat(actual)
              .usingRecursiveComparison().ignoringFields("id")
              .isEqualTo(order);
          assertThat(actual.getId())
              .isNotNull();

          TenantContext.setCurrentTenant(customerId);
          assertThat(orderRepository.findById(actual.getId()))
              .contains(actual);

          TenantContext.setCurrentTenant(customer2Id);
          assertThat(orderRepository.findById(actual.getId()))
              .isEmpty();
        });

    TenantContext.setCurrentTenant(customer2Id);
    assertThat(orderRepository.findAll())
        .isEmpty();
  }
}