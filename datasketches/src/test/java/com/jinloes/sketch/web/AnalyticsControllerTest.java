package com.jinloes.sketch.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jinloes.sketch.model.AnalyticsSummary;
import com.jinloes.sketch.model.Event;
import com.jinloes.sketch.model.PageFrequency;
import com.jinloes.sketch.model.ResponseTimeStats;
import com.jinloes.sketch.model.UniqueEstimate;
import com.jinloes.sketch.service.SketchService;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean SketchService sketchService;

  @Nested
  class RecordEvent {

    @Test
    void returnsAccepted() throws Exception {
      mockMvc
          .perform(
              post("/api/events")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new Event("user-1", "/home", 120.0))))
          .andExpect(status().isAccepted());

      then(sketchService).should().record(any(Event.class));
    }

    @Test
    void acceptsPartialEventWithNullFields() throws Exception {
      mockMvc
          .perform(
              post("/api/events")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(new Event(null, "/home", null))))
          .andExpect(status().isAccepted());
    }
  }

  @Nested
  class Summary {

    @Test
    void returnsFullAnalyticsSummary() throws Exception {
      given(sketchService.summary(anyInt()))
          .willReturn(
              new AnalyticsSummary(
                  new UniqueEstimate(1000, 980, 1020),
                  new UniqueEstimate(50, 48, 52),
                  new ResponseTimeStats(120.0, 350.0, 490.0, 5000),
                  List.of(new PageFrequency("/home", 2000))));

      mockMvc
          .perform(get("/api/analytics"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.uniqueVisitors.estimate").value(1000))
          .andExpect(jsonPath("$.uniquePages.estimate").value(50))
          .andExpect(jsonPath("$.responseTimes.p95").value(350.0))
          .andExpect(jsonPath("$.popularPages[0].page").value("/home"));
    }

    @Test
    void customLimitIsPassedToService() throws Exception {
      given(sketchService.summary(5))
          .willReturn(
              new AnalyticsSummary(
                  new UniqueEstimate(0, 0, 0),
                  new UniqueEstimate(0, 0, 0),
                  new ResponseTimeStats(0, 0, 0, 0),
                  List.of()));

      mockMvc.perform(get("/api/analytics?limit=5")).andExpect(status().isOk());

      then(sketchService).should().summary(5);
    }
  }

  @Nested
  class UniqueVisitors {

    @Test
    void returnsEstimateWithBounds() throws Exception {
      given(sketchService.uniqueVisitors()).willReturn(new UniqueEstimate(1000, 980, 1020));

      mockMvc
          .perform(get("/api/analytics/unique-visitors"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.estimate").value(1000))
          .andExpect(jsonPath("$.lowerBound").value(980))
          .andExpect(jsonPath("$.upperBound").value(1020));
    }
  }

  @Nested
  class UniquePages {

    @Test
    void returnsEstimateWithBounds() throws Exception {
      given(sketchService.uniquePages()).willReturn(new UniqueEstimate(50, 48, 52));

      mockMvc
          .perform(get("/api/analytics/unique-pages"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.estimate").value(50))
          .andExpect(jsonPath("$.lowerBound").value(48))
          .andExpect(jsonPath("$.upperBound").value(52));
    }
  }

  @Nested
  class ResponseTimes {

    @Test
    void returnsPercentiles() throws Exception {
      given(sketchService.responseTimes())
          .willReturn(new ResponseTimeStats(120.0, 350.0, 490.0, 5000));

      mockMvc
          .perform(get("/api/analytics/response-times"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.p50").value(120.0))
          .andExpect(jsonPath("$.p95").value(350.0))
          .andExpect(jsonPath("$.p99").value(490.0))
          .andExpect(jsonPath("$.count").value(5000));
    }
  }

  @Nested
  class PopularPages {

    @Test
    void returnsTopPages() throws Exception {
      given(sketchService.popularPages(anyInt()))
          .willReturn(List.of(new PageFrequency("/home", 2000), new PageFrequency("/about", 500)));

      mockMvc
          .perform(get("/api/analytics/popular-pages"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].page").value("/home"))
          .andExpect(jsonPath("$[0].estimate").value(2000))
          .andExpect(jsonPath("$[1].page").value("/about"));
    }

    @Test
    void customLimitIsPassedToService() throws Exception {
      given(sketchService.popularPages(3)).willReturn(List.of());

      mockMvc.perform(get("/api/analytics/popular-pages?limit=3")).andExpect(status().isOk());

      then(sketchService).should().popularPages(3);
    }
  }

  @Nested
  class ResetAnalytics {

    @Test
    void returnsNoContent() throws Exception {
      mockMvc.perform(delete("/api/analytics")).andExpect(status().isNoContent());

      then(sketchService).should().reset();
    }
  }
}
