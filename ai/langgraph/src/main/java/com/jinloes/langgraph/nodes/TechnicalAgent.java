package com.jinloes.langgraph.nodes;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface TechnicalAgent {

  @SystemMessage(
      """
            You answer technical support questions. Use the searchDocs tool to find relevant
            documentation BEFORE answering. If the tool returns NO_MATCH, tell the customer you
            could not find an answer and offer to escalate. Keep responses concise. Use the
            conversation history to track what's already been tried.
            """)
  String handle(@MemoryId String sessionId, @UserMessage String inquiry);
}
