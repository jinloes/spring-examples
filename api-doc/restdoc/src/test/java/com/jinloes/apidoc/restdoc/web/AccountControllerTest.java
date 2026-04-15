package com.jinloes.apidoc.restdoc.web;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinloes.apidoc.restdoc.model.CreateAccountRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Each test calls {@code document()} with a {@code resource()} snippet. The snippets are written to
 * {@code build/generated-snippets}, and the {@code openapi3} Gradle task assembles them into {@code
 * build/api-spec/openapi.yaml}.
 *
 * <p>If a field is added to the response but not described here, the test fails — the spec is
 * always in sync with the implementation.
 */
@WebMvcTest(AccountController.class)
@AutoConfigureRestDocs
class AccountControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void getAccount() throws Exception {
    mockMvc
        .perform(get("/accounts/{id}", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Sample Account"))
        .andDo(
            document(
                "get-account",
                resource(
                    ResourceSnippetParameters.builder()
                        .summary("Get account by ID")
                        .description("Returns a bank account for the given ID.")
                        .pathParameters(parameterWithName("id").description("Account UUID"))
                        .responseFields(
                            fieldWithPath("id").description("Account UUID"),
                            fieldWithPath("name").description("Account holder name"),
                            fieldWithPath("amount").description("Current balance in cents"))
                        .build())));
  }

  @Test
  void createAccount() throws Exception {
    mockMvc
        .perform(
            post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new CreateAccountRequest("Jane Doe", 50000))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Jane Doe"))
        .andDo(
            document(
                "create-account",
                resource(
                    ResourceSnippetParameters.builder()
                        .summary("Create a new account")
                        .requestFields(
                            fieldWithPath("name").description("Account holder name"),
                            fieldWithPath("initialAmount").description("Initial deposit in cents"))
                        .responseFields(
                            fieldWithPath("id").description("Generated account UUID"),
                            fieldWithPath("name").description("Account holder name"),
                            fieldWithPath("amount").description("Balance in cents"))
                        .build())));
  }
}
