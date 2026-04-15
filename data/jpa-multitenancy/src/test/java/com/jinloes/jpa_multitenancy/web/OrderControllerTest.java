package com.jinloes.jpa_multitenancy.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.jpa_multitenancy.TenantContext;
import com.jinloes.jpa_multitenancy.data.OrderRepository;
import com.jinloes.jpa_multitenancy.model.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderControllerTest {

  @Container
  private static final PostgreSQLContainer postgresContainer =
      new PostgreSQLContainer("postgres:13")
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

  static final String CUSTOMER_1 = "98b67afd-0d75-4c88-8d81-71821688f345";
  static final String CUSTOMER_2 = "98b67afd-0d75-4c88-8d81-71821688f346";

  @LocalServerPort int port;
  @Autowired OrderRepository orderRepository;
  WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    TenantContext.setCurrentTenant(CUSTOMER_1);
    orderRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  Order postOrder(String tenantId, String name) {
    return client
        .post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("X-Tenant", tenantId)
        .body(BodyInserters.fromValue(new Order().setName(name).setCustomerId(tenantId)))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(Order.class)
        .returnResult()
        .getResponseBody();
  }

  @Nested
  class CreateOrder {

    @Test
    void create_returnsCreatedOrderWithId() {
      Order created = postOrder(CUSTOMER_1, "Order 1");

      assertThat(created.getId()).isNotNull();
      assertThat(created.getName()).isEqualTo("Order 1");
    }
  }

  @Nested
  class TenantIsolation {

    @Test
    void order_isVisibleToOwningTenant() {
      Order created = postOrder(CUSTOMER_1, "Order 1");

      TenantContext.setCurrentTenant(CUSTOMER_1);
      assertThat(orderRepository.findById(created.getId())).contains(created);
    }

    @Test
    void order_isNotVisibleToOtherTenant() {
      Order created = postOrder(CUSTOMER_1, "Order 1");

      TenantContext.setCurrentTenant(CUSTOMER_2);
      assertThat(orderRepository.findById(created.getId())).isEmpty();
      assertThat(orderRepository.findAll()).isEmpty();
    }
  }
}
