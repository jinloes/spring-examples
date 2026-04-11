package com.jinloes.spring_examples.elasticsearch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AlarmControllerTest {

  // Security/SSL disabled; @DynamicPropertySource wires an explicit http:// URI so
  // the client doesn't try HTTPS. Cluster health wait ensures ES is fully ready
  // before Spring starts (avoids 400s during repository index-existence checks).
  @Container
  static ElasticsearchContainer elasticsearch =
      new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.0.0")
          .withEnv("xpack.security.enabled", "false")
          .withEnv("xpack.security.http.ssl.enabled", "false")
          .withEnv("discovery.type", "single-node")
          .waitingFor(
              Wait.forHttp("/_cluster/health?wait_for_status=yellow&timeout=30s")
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofMinutes(3)));

  @DynamicPropertySource
  static void elasticsearchProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.elasticsearch.uris", () -> "http://localhost:" + elasticsearch.getMappedPort(9200));
  }

  @Autowired private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Nested
  class CreateAlarm {

    @Test
    void create_returnsAlarmWithId() throws Exception {
      mockMvc
          .perform(
              post("/alarms")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      { "org": 42 }
                      """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.org").value(42));
    }
  }

  @Nested
  class GetAlarm {

    @Test
    void get_existingAlarm_returnsIt() throws Exception {
      MvcResult created =
          mockMvc
              .perform(
                  post("/alarms")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          """
                          { "org": 7 }
                          """))
              .andExpect(status().isOk())
              .andReturn();

      // Extract the id from the JSON response without pulling in a JSON library
      String response = created.getResponse().getContentAsString();
      String id = response.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

      mockMvc
          .perform(get("/alarms/{id}", id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id))
          .andExpect(jsonPath("$.org").value(7));
    }

    @Test
    void get_unknownId_returns404() throws Exception {
      mockMvc.perform(get("/alarms/nonexistent")).andExpect(status().isNotFound());
    }
  }
}
