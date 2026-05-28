package com.jinloes.langgraph;

import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.langgraph.observability.PendingActionsRegistry;
import com.jinloes.langgraph.observability.SessionContext;
import com.jinloes.langgraph.tools.RefundTool;
import com.jinloes.langgraph.web.UiAction;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefundToolTest {

  private final SessionContext session = new SessionContext();
  private final PendingActionsRegistry registry = new PendingActionsRegistry();
  private final RefundTool tool = new RefundTool(session, registry);

  @Test
  void offerRefundOptionsQueuesATwoOptionSelectAction() {
    session.set("sess-1");
    try {
      assertThat(tool.offerRefundOptions()).isEqualTo("OPTIONS_OFFERED");
    } finally {
      session.clear();
    }

    List<UiAction> queued = registry.drain("sess-1");
    assertThat(queued).hasSize(1);
    UiAction a = queued.get(0);
    assertThat(a.type()).isEqualTo("select");
    assertThat(a.options())
        .extracting(UiAction.Option::label)
        .containsExactly("Pick from a list", "Enter manually");
    // Values are natural-language so the classifier and refund agent can recognise them on
    // the next turn.
    assertThat(a.options())
        .extracting(UiAction.Option::value)
        .anyMatch(v -> v.toLowerCase().contains("list"))
        .anyMatch(v -> v.toLowerCase().contains("manual"));
  }

  @Test
  void listAvailableOrdersQueuesOnlyASelectAction() {
    session.set("sess-1");
    try {
      String summary = tool.listAvailableOrders();
      // The text payload (what the LLM sees) lists the order ids — useful so the model knows
      // what's available even though the UI shows the buttons.
      assertThat(summary).contains("ORD-1001").contains("ORD-1002").contains("ORD-1003");
    } finally {
      session.clear();
    }

    List<UiAction> queued = registry.drain("sess-1");
    // Only one action now — the "Enter manually" branch lives behind offerRefundOptions /
    // requestOrderId, so we no longer pair the list with an input field.
    assertThat(queued).hasSize(1);
    UiAction select = queued.get(0);
    assertThat(select.type()).isEqualTo("select");
    assertThat(select.options())
        .extracting(UiAction.Option::value)
        .contains("ORD-1001", "ORD-1002", "ORD-1003");
  }

  @Test
  void requestOrderIdQueuesAnInputAction() {
    session.set("sess-1");
    try {
      assertThat(tool.requestOrderId()).isEqualTo("ORDER_ID_INPUT_QUEUED");
    } finally {
      session.clear();
    }

    List<UiAction> queued = registry.drain("sess-1");
    assertThat(queued).hasSize(1);
    UiAction input = queued.get(0);
    assertThat(input.type()).isEqualTo("input");
    assertThat(input.placeholder()).isEqualTo("ORD-XXXX");
  }

  @Test
  void listAvailableOrdersSkipsActionWhenNoSessionInContext() {
    // Defensive: tool may be invoked outside a /triage call (e.g. an eval). It should still
    // return useful text without NPE on a missing session.
    String summary = tool.listAvailableOrders();
    assertThat(summary).contains("ORD-1001");
    assertThat(registry.drain("any")).isEmpty();
  }

  @Test
  void processRefundAutoApprovesSmallOrders() {
    String status = tool.processRefund("ORD-1001"); // $49.99
    assertThat(status).startsWith("APPROVED");
  }

  @Test
  void processRefundFlagsLargeOrdersForReview() {
    String status = tool.processRefund("ORD-1003"); // $1299.50
    assertThat(status).startsWith("PENDING_REVIEW");
  }

  @Test
  void processRefundReturnsUnknownForMissingOrder() {
    assertThat(tool.processRefund("ORD-9999")).isEqualTo("UNKNOWN_ORDER");
  }
}
