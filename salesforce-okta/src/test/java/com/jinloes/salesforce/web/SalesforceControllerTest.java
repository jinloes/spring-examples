package com.jinloes.salesforce.web;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinloes.salesforce.client.SalesforceClient;
import com.jinloes.salesforce.config.SecurityConfig;
import com.jinloes.salesforce.model.SalesforceQueryResult;
import com.jinloes.salesforce.model.SalesforceTokenResponse;
import com.jinloes.salesforce.service.SalesforceTokenService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SalesforceController.class)
@Import(SecurityConfig.class)
class SalesforceControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean SalesforceTokenService tokenService;
  @MockitoBean SalesforceClient salesforceClient;

  private static final String AUTH_HEADER = "Bearer test-okta-token";
  private SalesforceTokenResponse sfToken;

  @BeforeEach
  void setUp() {
    sfToken =
        new SalesforceTokenResponse(
            "sf-access-token", "https://linkedin--qa.sandbox.my.salesforce.com", "Bearer");
    when(tokenService.exchange("test-okta-token")).thenReturn(sfToken);
  }

  @Nested
  class QueryEndpoint {

    @Test
    void returnsResults() throws Exception {
      var result = new SalesforceQueryResult();
      result.setTotalSize(1);
      result.setDone(true);
      result.setRecords(List.of(Map.of("Id", "001xxx", "Subject", "Test Case")));
      when(salesforceClient.query(sfToken, "SELECT Id FROM Case LIMIT 1")).thenReturn(result);

      mockMvc
          .perform(
              get("/salesforce/query")
                  .param("soql", "SELECT Id FROM Case LIMIT 1")
                  .header("Authorization", AUTH_HEADER))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalSize").value(1))
          .andExpect(jsonPath("$.records[0].Id").value("001xxx"));
    }

    @Test
    void missingAuthHeader_returns400() throws Exception {
      mockMvc
          .perform(get("/salesforce/query").param("soql", "SELECT Id FROM Case LIMIT 1"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  class GetRecordEndpoint {

    @Test
    void returnsRecord() throws Exception {
      doReturn(Map.of("Id", "001xxx", "Subject", "Test Case"))
          .when(salesforceClient)
          .getRecord(sfToken, "Case", "001xxx");

      mockMvc
          .perform(get("/salesforce/sobjects/Case/001xxx").header("Authorization", AUTH_HEADER))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.Id").value("001xxx"));
    }
  }

  @Nested
  class CreateRecordEndpoint {

    @Test
    void returnsNewId() throws Exception {
      Map<String, Object> fields = Map.of("Subject", "New Case");
      when(salesforceClient.createRecord(sfToken, "Case", fields)).thenReturn("001newid");

      mockMvc
          .perform(
              post("/salesforce/sobjects/Case")
                  .header("Authorization", AUTH_HEADER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(fields)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value("001newid"));
    }
  }

  @Nested
  class UpdateRecordEndpoint {

    @Test
    void returns204() throws Exception {
      mockMvc
          .perform(
              patch("/salesforce/sobjects/Case/001xxx")
                  .header("Authorization", AUTH_HEADER)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(Map.of("Status", "Closed"))))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  class DeleteRecordEndpoint {

    @Test
    void returns204() throws Exception {
      mockMvc
          .perform(delete("/salesforce/sobjects/Case/001xxx").header("Authorization", AUTH_HEADER))
          .andExpect(status().isNoContent());
    }
  }
}
