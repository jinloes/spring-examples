package com.jinloes.langgraph.tools;

import com.jinloes.langgraph.observability.PendingActionsRegistry;
import com.jinloes.langgraph.observability.SessionContext;
import com.jinloes.langgraph.web.UiAction;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock refund processor + three UI-driven helpers that branch the refund flow:
 *
 * <ol>
 *   <li>{@link #offerRefundOptions()} — at the start of the flow, queues a 2-button choice ("Pick
 *       from a list" / "Enter manually") so the next turn can branch cleanly.
 *   <li>{@link #listAvailableOrders()} — taken when the customer picked the list option; queues a
 *       select UI action populated with the order book. The chat UI renders a client-side filter
 *       above the buttons when the list is longer than 3.
 *   <li>{@link #requestOrderId()} — taken when the customer chose to type manually; queues a
 *       text-input UI action only.
 * </ol>
 *
 * <p>{@link #processRefund(String)} is the action: approves orders &lt; $500 automatically, flags
 * larger ones for human review. Stateful so repeated calls for the same order are idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundTool {

  private static final double AUTO_APPROVE_THRESHOLD = 500.0;

  // LinkedHashMap to keep a deterministic display order. Several orders so the filter is useful.
  private final Map<String, Double> orderAmounts =
      orderedMap(
          "ORD-1001", 49.99,
          "ORD-1002", 219.00,
          "ORD-1003", 1299.50,
          "ORD-1004", 12.49,
          "ORD-1005", 87.30,
          "ORD-1006", 749.99,
          "ORD-1007", 39.00);

  private final Map<String, String> refundStatus = new ConcurrentHashMap<>();

  private final SessionContext sessionContext;
  private final PendingActionsRegistry pendingActions;

  @Tool(
      "Process a refund for the given order id. Returns the resulting status: APPROVED, "
          + "PENDING_REVIEW (needs human approval), or UNKNOWN_ORDER.")
  public String processRefund(@P("Order id, e.g. ORD-1001") String orderId) {
    log.info("RefundTool.processRefund order=%s".formatted(orderId));

    Double amount = orderAmounts.get(orderId);
    if (amount == null) {
      return "UNKNOWN_ORDER";
    }

    // Auto-approve small refunds; route larger ones to a human queue.
    String status =
        amount < AUTO_APPROVE_THRESHOLD
            ? "APPROVED ($%.2f refunded)".formatted(amount)
            : "PENDING_REVIEW ($%.2f requires manager approval)".formatted(amount);

    refundStatus.put(orderId, status);
    return status;
  }

  @Tool(
      "Offer the customer a choice of HOW to identify their order: pick from a list or enter "
          + "the id manually. Call this at the START of a refund flow when the customer "
          + "hasn't specified an order yet. Queues a 2-button picker on the chat UI; your text "
          + "response only needs to briefly ask how they'd like to find the order.")
  public String offerRefundOptions() {
    log.info("RefundTool.offerRefundOptions");

    String sid = sessionContext.get();
    if (sid != null) {
      // Values include "refund" explicitly — when the user clicks a button, this value goes
      // as the next inquiry and gets re-classified. Without "refund" in the text, the
      // classifier (Qwen3.6 in particular) sometimes routes the continuation to "clarify",
      // breaking the flow. The UI hides this by showing the shorter `label` in the chat
      // bubble; only the server-side classifier sees the longer value.
      pendingActions.add(
          sid,
          UiAction.select(
              "How would you like to find the order?",
              List.of(
                  new UiAction.Option(
                      "I'd like to pick from a list of orders to refund", "Pick from a list"),
                  new UiAction.Option(
                      "I'd like to manually enter the order id to refund", "Enter manually"))));
    }
    return "OPTIONS_OFFERED";
  }

  @Tool(
      "List the customer's refundable orders. Call this when the customer has chosen to PICK "
          + "FROM A LIST (their latest message will say something like 'show me the list'). "
          + "Queues a clickable list on the chat UI; your text response only needs to briefly "
          + "tell the customer to pick one. Do not enumerate the ids yourself.")
  public String listAvailableOrders() {
    log.info("RefundTool.listAvailableOrders");

    String summary =
        orderAmounts.entrySet().stream()
            .map(e -> "%s ($%.2f)".formatted(e.getKey(), e.getValue()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("(no orders)");

    String sid = sessionContext.get();
    if (sid != null) {
      List<UiAction.Option> options =
          orderAmounts.entrySet().stream()
              .map(
                  e ->
                      new UiAction.Option(
                          e.getKey(), "%s — $%.2f".formatted(e.getKey(), e.getValue())))
              .toList();
      pendingActions.add(sid, UiAction.select("Pick an order to refund:", options));
    }

    return summary;
  }

  @Tool(
      "Prompt the customer to type their order id. Call this when the customer has chosen to "
          + "ENTER THEIR ORDER ID MANUALLY (their latest message will say something like 'let "
          + "me enter it'). Queues a text-input field on the chat UI; your text response only "
          + "needs to briefly ask them to type the id.")
  public String requestOrderId() {
    log.info("RefundTool.requestOrderId");

    String sid = sessionContext.get();
    if (sid != null) {
      pendingActions.add(sid, UiAction.input("Type the order id:", "ORD-XXXX"));
    }
    return "ORDER_ID_INPUT_QUEUED";
  }

  private static Map<String, Double> orderedMap(Object... kv) {
    LinkedHashMap<String, Double> m = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
    }
    return m;
  }
}
