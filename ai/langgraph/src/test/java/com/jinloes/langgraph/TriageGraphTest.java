package com.jinloes.langgraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jinloes.langgraph.graph.TriageState;
import com.jinloes.langgraph.nodes.BillingAgent;
import com.jinloes.langgraph.nodes.ClarifyAgent;
import com.jinloes.langgraph.nodes.IntentClassifier;
import com.jinloes.langgraph.nodes.RefundAgent;
import com.jinloes.langgraph.nodes.TechnicalAgent;
import java.util.Map;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * End-to-end test of the state graph with all AiService beans mocked. Verifies routing through the
 * conditional edges, the clarify-loop guard, and that the sessionId reaches each agent so its
 * {@code ChatMemoryProvider} can scope memory per conversation.
 */
@SpringBootTest(classes = SupportTriageApplication.class)
@TestPropertySource(
    properties = {
      "langsmith.enabled=false",
      // Provide a dummy API key so the Gemini starter doesn't fail validation; the
      // ChatModel is never invoked because we mock all the AiService beans.
      "langchain4j.google-ai-gemini.chat-model.api-key=test",
    })
class TriageGraphTest {

  private static final String SID = "test-session-1";

  @Autowired CompiledGraph<TriageState> triageGraph;
  @MockitoBean IntentClassifier intentClassifier;
  @MockitoBean RefundAgent refundAgent;
  @MockitoBean TechnicalAgent technicalAgent;
  @MockitoBean BillingAgent billingAgent;
  @MockitoBean ClarifyAgent clarifyAgent;

  @Test
  void refundWithOrderIdTakesDeterministicPath() {
    // When the inquiry already contains an ORD-NNNN, the refund node bypasses the LLM agent
    // and calls RefundTool.processRefund directly. This makes the actual refund action
    // reliable regardless of how Gemini behaves (e.g. emitting tool calls without follow-up
    // text). RefundTool is the real bean here, so we hit the in-memory order book.
    when(intentClassifier.classify(anyString(), anyString())).thenReturn("refund");

    TriageState finalState = invoke("I want a refund for ORD-1001");

    assertThat(finalState.intent()).hasValue("refund");
    // ORD-1001 is $49.99, under the auto-approve threshold.
    assertThat(finalState.response().orElseThrow())
        .contains("ORD-1001")
        .contains("approved")
        .contains("$49.99");
    verify(refundAgent, never()).handle(anyString(), anyString());
  }

  @Test
  void refundWithoutOrderIdOffersTheChoiceButtonsDeterministically() {
    // No ORD-NNNN in the inquiry and no "list"/"manual" choice signal — the refund node
    // deterministically queues the offer-options action and bypasses the LLM agent. Avoids
    // the failure mode where the agent writes "Sure — how would you find the order?" but
    // skips calling offerRefundOptions, leaving the UI button-less.
    when(intentClassifier.classify(anyString(), anyString())).thenReturn("refund");

    TriageState finalState = invoke("I want a refund");

    assertThat(finalState.intent()).hasValue("refund");
    assertThat(finalState.response().orElseThrow()).contains("how would you like");
    verify(refundAgent, never()).handle(anyString(), anyString());
  }

  @Test
  void refundWithListSignalGoesThroughTheAgent() {
    // The user has clicked "Pick from a list" — the value sent is the natural-language
    // string we configured. Refund node detects the "list" signal and routes to the LLM
    // agent so it can call listAvailableOrders.
    when(intentClassifier.classify(anyString(), anyString())).thenReturn("refund");
    when(refundAgent.handle(anyString(), anyString())).thenReturn("Sure, pick an order below.");

    TriageState finalState = invoke("Show me the list of orders to refund");

    assertThat(finalState.intent()).hasValue("refund");
    assertThat(finalState.response().orElseThrow()).contains("pick");
    verify(refundAgent).handle(eq(SID), eq("Show me the list of orders to refund"));
  }

  @Test
  void routesTechnicalIntentToTechnicalAgent() {
    when(intentClassifier.classify(anyString(), anyString())).thenReturn("technical");
    when(technicalAgent.handle(anyString(), anyString()))
        .thenReturn("Reset your password at /account/reset.");

    TriageState finalState = invoke("I can't log in");

    assertThat(finalState.intent()).hasValue("technical");
    assertThat(finalState.response().orElseThrow()).contains("Reset your password");
  }

  @Test
  void routesBillingIntentToBillingAgent() {
    when(intentClassifier.classify(anyString(), anyString())).thenReturn("billing");
    when(billingAgent.handle(anyString(), anyString()))
        .thenReturn("Your plan is Pro, balance $0.00.");

    TriageState finalState = invoke("How much do I owe?");

    assertThat(finalState.intent()).hasValue("billing");
    assertThat(finalState.response().orElseThrow()).contains("Pro");
  }

  @Test
  void clarifyIntentEndsAfterOneClarifyingQuestion() {
    // Each /triage call handles exactly one user turn; no in-graph clarify loop. The
    // cross-turn cycle is provided by the chat client via ChatMemory.
    when(intentClassifier.classify(anyString(), anyString())).thenReturn("clarify");
    when(clarifyAgent.askClarifyingQuestion(anyString(), anyString()))
        .thenReturn("I can help with refunds, technical issues, or billing — which fits?");

    TriageState finalState = invoke("help");

    assertThat(finalState.intent()).hasValue("clarify");
    assertThat(finalState.response().orElseThrow()).contains("refunds");
  }

  private TriageState invoke(String inquiry) {
    return triageGraph
        .invoke(Map.of(TriageState.INQUIRY, inquiry, TriageState.SESSION_ID, SID))
        .orElseThrow();
  }
}
