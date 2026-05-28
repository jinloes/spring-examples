package com.jinloes.langgraph.nodes;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Classifies a customer-support inquiry into a routing label. The classifier carries its own chat
 * memory namespaced by session id, so on a follow-up turn like "ORD-1234" it can still recall the
 * previous classification ("refund") and route correctly.
 */
public interface IntentClassifier {

  @SystemMessage(
      """
            You are a customer support router.
            Classify the LATEST user message in context into EXACTLY ONE of these labels
            (lowercase, no other text):
              refund    - the customer wants money back for a specific order
              technical - the customer is reporting a bug, login problem, API issue, or how-to question
              billing   - the customer is asking about their plan, charges, or account balance
              clarify   - the inquiry is too vague or ambiguous to route confidently
            If the message is a short follow-up (an order id, an email, etc.), re-use the most
            recent intent from the conversation rather than returning clarify.
            Return only the label.
            """)
  String classify(@MemoryId String sessionId, @UserMessage String inquiry);
}
