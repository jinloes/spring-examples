package com.jinloes.langgraph.config;

import com.jinloes.langgraph.nodes.BillingAgent;
import com.jinloes.langgraph.nodes.ClarifyAgent;
import com.jinloes.langgraph.nodes.IntentClassifier;
import com.jinloes.langgraph.nodes.RefundAgent;
import com.jinloes.langgraph.nodes.TechnicalAgent;
import com.jinloes.langgraph.tools.AccountTool;
import com.jinloes.langgraph.tools.DocsSearchTool;
import com.jinloes.langgraph.tools.RefundTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires each agent interface to the auto-configured {@link ChatModel}, attaching tools and a
 * per-agent {@link ChatMemoryProvider}.
 *
 * <p>Memory design: every agent shares one in-memory {@link ChatMemoryStore} bean, but each agent
 * namespaces its memory id ({@code "refund:" + sessionId} etc.) so their conversation histories
 * stay isolated. This avoids the duplicate-user-message problem you'd get from sharing a single
 * memory across agents — every {@code AiServices} call appends the user message before sending, so
 * two agents on the same memory would each add it once per turn.
 *
 * <p>The classifier carrying its own memory is intentional: on a follow-up turn like "ORD-1234",
 * the classifier needs to recall the previous "refund" classification to route correctly.
 */
@Configuration
public class AgentsConfig {

  /** Cap context size: 20 messages = ~10 user/assistant turns. Plenty for support chats. */
  private static final int WINDOW = 20;

  @Bean
  public ChatMemoryStore chatMemoryStore() {
    return new InMemoryChatMemoryStore();
  }

  private static ChatMemoryProvider memoryFor(String prefix, ChatMemoryStore store) {
    return memoryId ->
        MessageWindowChatMemory.builder()
            .id(prefix + ":" + memoryId)
            .maxMessages(WINDOW)
            .chatMemoryStore(store)
            .build();
  }

  @Bean
  public IntentClassifier intentClassifier(ChatModel model, ChatMemoryStore store) {
    return AiServices.builder(IntentClassifier.class)
        .chatModel(model)
        .chatMemoryProvider(memoryFor("classifier", store))
        .build();
  }

  @Bean
  public RefundAgent refundAgent(ChatModel model, ChatMemoryStore store, RefundTool tool) {
    return AiServices.builder(RefundAgent.class)
        .chatModel(model)
        .chatMemoryProvider(memoryFor("refund", store))
        .tools(tool)
        .build();
  }

  @Bean
  public TechnicalAgent technicalAgent(
      ChatModel model, ChatMemoryStore store, DocsSearchTool tool) {
    return AiServices.builder(TechnicalAgent.class)
        .chatModel(model)
        .chatMemoryProvider(memoryFor("technical", store))
        .tools(tool)
        .build();
  }

  @Bean
  public BillingAgent billingAgent(ChatModel model, ChatMemoryStore store, AccountTool tool) {
    return AiServices.builder(BillingAgent.class)
        .chatModel(model)
        .chatMemoryProvider(memoryFor("billing", store))
        .tools(tool)
        .build();
  }

  @Bean
  public ClarifyAgent clarifyAgent(ChatModel model, ChatMemoryStore store) {
    return AiServices.builder(ClarifyAgent.class)
        .chatModel(model)
        .chatMemoryProvider(memoryFor("clarify", store))
        .build();
  }
}
