package com.jinloes.langgraph.graph;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import com.jinloes.langgraph.nodes.BillingAgent;
import com.jinloes.langgraph.nodes.ClarifyAgent;
import com.jinloes.langgraph.nodes.IntentClassifier;
import com.jinloes.langgraph.nodes.RefundAgent;
import com.jinloes.langgraph.nodes.TechnicalAgent;
import com.jinloes.langgraph.observability.LangSmithGraphHooks;
import com.jinloes.langgraph.observability.SessionContext;
import com.jinloes.langgraph.tools.RefundTool;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up the support-triage state graph:
 *
 * <pre>
 *   START -> classify_intent -> (refund | technical | billing | clarify) -> END
 * </pre>
 *
 * <p>Each graph invocation handles exactly one user turn. The cross-turn cycle — user types, agent
 * answers, user types again — is provided by the chat client + {@code ChatMemory}, not by looping
 * within the graph. Looping {@code clarify -> classify} inside one invocation would just
 * re-classify the same unchanged input and burn LLM calls for no benefit.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TriageGraph {

  private static final Set<String> VALID_INTENTS =
      Set.of("refund", "technical", "billing", "clarify");

  /** Matches order ids like ORD-1001 anywhere in the message. */
  private static final Pattern ORDER_ID = Pattern.compile("\\bORD-\\d{4,}\\b");

  private final IntentClassifier intentClassifier;
  private final RefundAgent refundAgent;
  private final TechnicalAgent technicalAgent;
  private final BillingAgent billingAgent;
  private final ClarifyAgent clarifyAgent;
  private final LangSmithGraphHooks graphHooks;
  private final SessionContext sessionContext;
  private final RefundTool refundTool;

  @Bean
  public CompiledGraph<TriageState> compiledTriageGraph() throws GraphStateException {

    NodeAction<TriageState> classify =
        state -> {
          String raw =
              intentClassifier.classify(state.sessionId(), state.inquiry()).trim().toLowerCase();
          // Defensive: clamp the LLM's output to a known label.
          String intent = VALID_INTENTS.contains(raw) ? raw : "clarify";
          log.info("classify_intent: inquiry='%s' -> intent=%s".formatted(state.inquiry(), intent));
          return Map.of(TriageState.INTENT, intent);
        };

    NodeAction<TriageState> refund =
        state -> {
          sessionContext.set(state.sessionId());
          try {
            String inquiry = state.inquiry();

            // 1. Inquiry contains an order id (e.g. user clicked a picker button or typed
            //    "ORD-1234") → process the refund deterministically. Sensitive financial
            //    action, never trust the LLM to phrase the outcome.
            Matcher m = ORDER_ID.matcher(inquiry);
            if (m.find()) {
              String orderId = m.group();
              return respond(formatRefundResult(orderId, refundTool.processRefund(orderId)));
            }

            // 2. Inquiry has no "list" / "manual" choice signal yet → this is the start of
            //    the flow, deterministically offer the two-button choice. More reliable than
            //    asking the LLM to call offerRefundOptions, which Qwen3.6 in particular tends
            //    to skip in favour of writing the suggested text directly.
            String lower = inquiry.toLowerCase();
            boolean hasChoiceSignal =
                lower.contains("list")
                    || lower.contains("show me")
                    || lower.contains("manual")
                    || lower.contains("enter the order");
            if (!hasChoiceSignal) {
              refundTool.offerRefundOptions();
              return respond("Sure — how would you like to find your order?");
            }

            // 3. User has signalled a preference — defer to the LLM agent, whose prompt knows
            //    to dispatch to listAvailableOrders or requestOrderId based on the message.
            return respond(refundAgent.handle(state.sessionId(), inquiry));
          } finally {
            sessionContext.clear();
          }
        };

    NodeAction<TriageState> technical =
        state -> respond(technicalAgent.handle(state.sessionId(), state.inquiry()));

    NodeAction<TriageState> billing =
        state -> respond(billingAgent.handle(state.sessionId(), state.inquiry()));

    NodeAction<TriageState> clarify =
        state -> respond(clarifyAgent.askClarifyingQuestion(state.sessionId(), state.inquiry()));

    var graph =
        new StateGraph<>(TriageState.SCHEMA, TriageState::new)
            .addNode("classify_intent", node_async(classify))
            .addNode("refund", node_async(refund))
            .addNode("technical", node_async(technical))
            .addNode("billing", node_async(billing))
            .addNode("clarify", node_async(clarify))
            .addEdge(START, "classify_intent")
            .addConditionalEdges(
                "classify_intent",
                edge_async(state -> state.intent().orElse("clarify")),
                Map.of(
                    "refund", "refund",
                    "technical", "technical",
                    "billing", "billing",
                    "clarify", "clarify"))
            .addEdge("refund", END)
            .addEdge("technical", END)
            .addEdge("billing", END)
            .addEdge("clarify", END);

    graphHooks.attach(graph);

    return graph.compile();
  }

  /**
   * Wrap an agent's text reply for the state update. The LLM sometimes returns null when it only
   * called a tool and produced no follow-up text (a UI action handles the user's next step); fall
   * back to an empty string so {@code Map.of} doesn't NPE.
   */
  private static Map<String, Object> respond(String text) {
    return Map.of(TriageState.RESPONSE, text == null ? "" : text);
  }

  /** Turn the raw tool status string into a customer-facing sentence. */
  private static String formatRefundResult(String orderId, String toolResult) {
    if (toolResult.startsWith("APPROVED")) {
      // toolResult looks like "APPROVED ($49.99 refunded)" — keep the amount.
      String amount = toolResult.substring("APPROVED ".length());
      return "Your refund for %s has been approved %s.".formatted(orderId, amount);
    }
    if (toolResult.startsWith("PENDING_REVIEW")) {
      // toolResult looks like "PENDING_REVIEW ($219.00 requires manager approval)".
      String detail = toolResult.substring("PENDING_REVIEW ".length());
      return "Your refund for %s is pending — %s.".formatted(orderId, detail);
    }
    if ("UNKNOWN_ORDER".equals(toolResult)) {
      return "I couldn't find order %s. Could you double-check the id?".formatted(orderId);
    }
    return toolResult;
  }
}
