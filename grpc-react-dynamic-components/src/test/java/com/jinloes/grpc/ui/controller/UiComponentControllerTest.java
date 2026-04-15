package com.jinloes.grpc.ui.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class UiComponentControllerTest {

  @Autowired private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = webAppContextSetup(wac).build();
  }

  @Nested
  class GetComponents {

    @Test
    void returnsComponentSchema() throws Exception {
      mockMvc
          .perform(get("/api/components").param("ids", "reroute-case"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].componentId").value("reroute-case"))
          .andExpect(jsonPath("$[0].type").value("form"))
          .andExpect(jsonPath("$[0].fields").isArray());
    }
  }

  @Nested
  class ResolveState {

    @Test
    void defaultTriggerValue_conditionalFieldsExcluded() throws Exception {
      // rerouteTo = "--Select--": only rerouteTo and remarks have no conditional hide
      mockMvc
          .perform(
              post("/api/components/state")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"componentId":"reroute-case","fieldValues":{"rerouteTo":"--Select--"}}
                      """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.componentId").value("reroute-case"))
          .andExpect(jsonPath("$.fields[?(@.name=='rerouteTo')]").exists())
          .andExpect(jsonPath("$.fields[?(@.name=='remarks')]").exists())
          .andExpect(jsonPath("$.fields[?(@.name=='rerouteReason')]").doesNotExist())
          .andExpect(jsonPath("$.fields[?(@.name=='caseType')]").doesNotExist());
    }

    @Test
    void differentSupportTeam_equalsAndInFieldsIncluded() throws Exception {
      mockMvc
          .perform(
              post("/api/components/state")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"componentId":"reroute-case","fieldValues":{"rerouteTo":"Different Support Team"}}
                      """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.fields[?(@.name=='rerouteReason')]").exists())
          .andExpect(jsonPath("$.fields[?(@.name=='caseType')]").exists())
          .andExpect(jsonPath("$.fields[?(@.name=='ownerTeamMember')]").exists());
    }

    @Test
    void ownerTeamMember_inOperatorMatchesBothListValues() throws Exception {
      mockMvc
          .perform(
              post("/api/components/state")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"componentId":"reroute-case","fieldValues":{"rerouteTo":"Owner Team Member"}}
                      """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.fields[?(@.name=='ownerTeamMember')]").exists())
          .andExpect(jsonPath("$.fields[?(@.name=='caseType')]").doesNotExist());
    }

    @Test
    void unknownComponent_returns404() throws Exception {
      mockMvc
          .perform(
              post("/api/components/state")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"componentId":"does-not-exist","fieldValues":{}}
                      """))
          .andExpect(status().isNotFound());
    }
  }
}
