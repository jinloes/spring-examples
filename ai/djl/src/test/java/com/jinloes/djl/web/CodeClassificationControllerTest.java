package com.jinloes.djl.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.djl.modality.Classifications;
import com.jinloes.djl.service.DjlService;
import com.jinloes.djl.web.CodeClassificationController.ClassificationRequest;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Controller-layer tests using a mocked {@link DjlService}. No model files are required. For
 * end-to-end inference tests, download the model per CLAUDE.md and run against the full Spring
 * context.
 */
@WebMvcTest(CodeClassificationController.class)
class CodeClassificationControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean DjlService djlService;

  // ---------------------------------------------------------------------------
  // Successful classification
  // ---------------------------------------------------------------------------

  @Nested
  class WhenClassificationSucceeds {

    @Test
    void returnsAllLanguagesWithProbabilities() throws Exception {
      given(djlService.classify(anyString()))
          .willReturn(new Classifications(List.of("java", "javascript"), List.of(0.9999, 0.0001)));

      mockMvc
          .perform(
              post("/classify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          new ClassificationRequest(
                              "public class Test { public static void main(String[] args) {} }"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.classifications.java").value(0.9999))
          .andExpect(jsonPath("$.classifications.javascript").value(0.0001));
    }

    @Test
    void resultsSortedByProbabilityDescending() throws Exception {
      // Deliberately provide classifications out of order to verify sorting.
      given(djlService.classify(anyString()))
          .willReturn(
              new Classifications(List.of("php", "java", "python"), List.of(0.1, 0.7, 0.2)));

      var result =
          mockMvc
              .perform(
                  post("/classify")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(objectMapper.writeValueAsString(new ClassificationRequest("code"))))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.classifications.java").value(0.7))
              .andExpect(jsonPath("$.classifications.python").value(0.2))
              .andExpect(jsonPath("$.classifications.php").value(0.1))
              .andReturn();

      // Verify key order in the serialized JSON reflects descending probability.
      String body = result.getResponse().getContentAsString();
      assertThat(body.indexOf("\"java\""))
          .as("java should appear before python")
          .isLessThan(body.indexOf("\"python\""));
      assertThat(body.indexOf("\"python\""))
          .as("python should appear before php")
          .isLessThan(body.indexOf("\"php\""));
    }

    @Test
    void singleLanguageResultReturnsOneEntry() throws Exception {
      given(djlService.classify(anyString()))
          .willReturn(new Classifications(List.of("go"), List.of(1.0)));

      mockMvc
          .perform(
              post("/classify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          new ClassificationRequest("package main\nfunc main() {}"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.classifications.go").value(1.0))
          .andExpect(jsonPath("$.classifications").isMap());
    }
  }

  // ---------------------------------------------------------------------------
  // Absent or blank code
  // ---------------------------------------------------------------------------

  @Nested
  class WhenCodeIsAbsent {

    @Test
    void emptyStringReturnsEmptyMap() throws Exception {
      given(djlService.classify("")).willReturn(new Classifications(List.of(), List.of()));

      mockMvc
          .perform(
              post("/classify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new ClassificationRequest(""))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.classifications").isEmpty());
    }

    @Test
    void nullCodeReturnsEmptyMap() throws Exception {
      given(djlService.classify(null)).willReturn(new Classifications(List.of(), List.of()));

      mockMvc
          .perform(
              post("/classify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new ClassificationRequest(null))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.classifications").isEmpty());
    }

    @Test
    void missingCodeFieldReturnsEmptyMap() throws Exception {
      // Omitting the "code" field causes Jackson to set code=null in the record.
      given(djlService.classify(null)).willReturn(new Classifications(List.of(), List.of()));

      mockMvc
          .perform(post("/classify").contentType(MediaType.APPLICATION_JSON).content("{}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.classifications").isEmpty());
    }
  }

  // ---------------------------------------------------------------------------
  // Service failures
  // ---------------------------------------------------------------------------

  @Nested
  class WhenServiceFails {

    @Test
    void runtimeExceptionReturnsInternalServerError() throws Exception {
      given(djlService.classify(anyString()))
          .willThrow(new RuntimeException("Failed to classify code snippet"));

      mockMvc
          .perform(
              post("/classify")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new ClassificationRequest("some code"))))
          .andExpect(status().isInternalServerError());
    }
  }

  // ---------------------------------------------------------------------------
  // Invalid requests
  // ---------------------------------------------------------------------------

  @Nested
  class WhenRequestIsInvalid {

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
      mockMvc
          .perform(
              post("/classify").contentType(MediaType.APPLICATION_JSON).content("{not valid json"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void emptyBodyReturnsBadRequest() throws Exception {
      mockMvc
          .perform(post("/classify").contentType(MediaType.APPLICATION_JSON).content(""))
          .andExpect(status().isBadRequest());
    }

    @Test
    void missingContentTypeReturnsUnsupportedMediaType() throws Exception {
      mockMvc
          .perform(post("/classify").content("{\"code\":\"test\"}"))
          .andExpect(status().isUnsupportedMediaType());
    }
  }
}
