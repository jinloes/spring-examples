package com.jinloes.messaging.pubsub.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Unit test for the publish endpoint — mocks PubSubTemplate so no GCP credentials are required.
 * Running the full application requires a GCP project and credentials set via ENCODED_KEY env var.
 */
@WebMvcTest(MessageController.class)
class MessageControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean PubSubTemplate pubSubTemplate;

  @Test
  void publishReturnsAccepted() throws Exception {
    mockMvc
        .perform(
            post("/api/topics/my-topic").contentType(MediaType.TEXT_PLAIN).content("hello world"))
        .andExpect(status().isAccepted());
  }
}
