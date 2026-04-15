package com.jinloes.apidoc.openapi.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinloes.apidoc.openapi.model.CreateAccountRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void getAccountReturnsAccountWithMatchingId() throws Exception {
    UUID id = UUID.randomUUID();
    MvcResult result =
        mockMvc
            .perform(get("/accounts/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Sample Account"))
            .andExpect(jsonPath("$.amount").value(1000))
            .andReturn();

    String responseId =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    assertThat(responseId).isEqualTo(id.toString());
  }

  @Test
  void createAccountReturnsCreatedAccountWithRequestedValues() throws Exception {
    mockMvc
        .perform(
            post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new CreateAccountRequest("Jane Doe", 50000))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Jane Doe"))
        .andExpect(jsonPath("$.amount").value(50000));
  }
}
