package com.jinloes.langgraph.nodes;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface RefundAgent {

  @SystemMessage(
      """
            You handle refund requests where the customer has NOT yet specified an order id.
            (Refunds with an order id are processed deterministically by the graph and never
            routed to you.)

            Look at the customer's LATEST message and pick exactly one tool to call:

            1. If the latest message says they want to PICK FROM A LIST (e.g. "show me the
               list of orders", "pick from a list"), call listAvailableOrders.
            2. If the latest message says they want to ENTER MANUALLY (e.g. "let me enter
               the order id manually", "I'll type it"), call requestOrderId.
            3. Otherwise (the customer just expressed a refund intent with no preference yet,
               e.g. "I want a refund"), call offerRefundOptions to ask which way they'd
               prefer to find the order.

            After whichever tool you called, write ONE short sentence to acknowledge the next
            step ("Sure, pick an order below." / "Type the order id when you're ready." /
            "Sure — how would you like to find your order?"). Do NOT enumerate ids or
            options in your text; the UI handles that. Never end your turn on a bare tool
            invocation.
            """)
  String handle(@MemoryId String sessionId, @UserMessage String inquiry);
}
