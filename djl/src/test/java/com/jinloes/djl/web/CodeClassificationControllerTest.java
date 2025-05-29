package com.jinloes.djl.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CodeClassificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private String javaCode;
  private String pythonCode;
  private String emptyCode;

  @BeforeEach
  void setUp() {
    javaCode = "public class Test { public void test() { System.out.println(\"Hello World\"); } }";
    pythonCode = "def hello_world():\n    print(\"Hello World\")\n\nif __name__ == \"__main__\":\n    hello_world()";
    emptyCode = "";
  }

  @Test
  void javaCode() throws Exception {
    CodeClassificationController.ClassificationRequest request =
        new CodeClassificationController.ClassificationRequest(javaCode);

    MvcResult result = mockMvc.perform(post("/classify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    CodeClassificationController.CodeClassifications classifications = objectMapper.readValue(responseJson,
        CodeClassificationController.CodeClassifications.class);

    assertThat(classifications.classifications().entrySet())
        .startsWith(entry("java", .9999755620956421), entry("javascript", 2.3112208509701304E-5));
  }

  @Test
  void pythonCode() throws Exception {
    CodeClassificationController.ClassificationRequest request =
        new CodeClassificationController.ClassificationRequest(pythonCode);

    MvcResult result = mockMvc.perform(post("/classify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    CodeClassificationController.CodeClassifications classifications = objectMapper.readValue(responseJson,
        CodeClassificationController.CodeClassifications.class);

    assertThat(classifications.classifications().entrySet())
        .startsWith(entry("python", .9999967813491821), entry("ruby", 1.8028921431323397E-6));
  }

  @Test
  void emptyCode() throws Exception {
    CodeClassificationController.ClassificationRequest request =
        new CodeClassificationController.ClassificationRequest(emptyCode);

    MvcResult result = mockMvc.perform(post("/classify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    CodeClassificationController.CodeClassifications classifications = objectMapper.readValue(responseJson,
        CodeClassificationController.CodeClassifications.class);

    assertThat(classifications.classifications())
        .isEmpty();
  }

  @Test
  void nullCode() throws Exception {
    CodeClassificationController.ClassificationRequest request =
        new CodeClassificationController.ClassificationRequest(null);

    MvcResult result = mockMvc.perform(post("/classify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    CodeClassificationController.CodeClassifications classifications = objectMapper.readValue(responseJson,
        CodeClassificationController.CodeClassifications.class);

    assertThat(classifications.classifications())
        .isEmpty();
  }

  @Test
  void mixedLanguageCode() throws Exception {
    String mixedCode = javaCode + "\n\n" + pythonCode;
    CodeClassificationController.ClassificationRequest request =
        new CodeClassificationController.ClassificationRequest(mixedCode);

    MvcResult result = mockMvc.perform(post("/classify")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String responseJson = result.getResponse().getContentAsString();
    CodeClassificationController.CodeClassifications classifications = objectMapper.readValue(responseJson,
        CodeClassificationController.CodeClassifications.class);

    assertThat(classifications.classifications().entrySet())
        .startsWith(entry("java", .9252889156341553), entry("python", .0673179030418396));
  }
}
