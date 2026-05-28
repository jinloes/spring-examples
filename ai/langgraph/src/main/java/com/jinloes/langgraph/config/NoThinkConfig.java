package com.jinloes.langgraph.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * When the lm-studio profile is active, swap the auto-configured {@link OpenAiChatModel} for a
 * {@link NoThinkChatModel} wrapper that prefills an empty {@code <think></think>} assistant turn —
 * see that class for why this matters for Qwen3-thinking-family models that ignore {@code
 * /no_think}.
 *
 * <p>{@link Primary} ensures every injection point ({@link AgentsConfig}, {@link
 * com.jinloes.langgraph.config.LmStudioWarmup}, etc.) picks the wrapper over the raw OpenAI bean.
 * The raw bean is still in the context; we just hide it behind the no-think wrapper.
 */
@Configuration
@Profile("lm-studio")
public class NoThinkConfig {

  @Bean
  @Primary
  public ChatModel noThinkChatModel(OpenAiChatModel openAiChatModel) {
    return new NoThinkChatModel(openAiChatModel);
  }
}
