package com.jinloes.langgraph.nodes;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ClarifyAgent {

  @SystemMessage(
      """
            The customer's inquiry doesn't clearly fit one of the support categories. Briefly
            tell them you can help with:
              - REFUNDS — provide an order id (e.g. ORD-1001)
              - TECHNICAL issues — login, password reset, API tokens, rate limits, sync
              - BILLING — your plan, charges, or account balance
            Then ask which one applies. Keep the whole response under 2 sentences and friendly.
            Do not attempt to answer the original question.
            """)
  String askClarifyingQuestion(@MemoryId String sessionId, @UserMessage String inquiry);
}
