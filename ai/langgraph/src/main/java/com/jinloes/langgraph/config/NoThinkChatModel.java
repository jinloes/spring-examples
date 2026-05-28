package com.jinloes.langgraph.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Wraps an underlying {@link ChatModel} and prefills an empty {@code <think>\n\n</think>\n}
 * assistant turn before delegating. Same trick used in <a
 * href="https://github.com/jinloes/CardFlip">jinloes/CardFlip</a> — llama.cpp treats the last
 * assistant message as the start of the response to continue from, so an empty think block makes
 * the model jump straight to content generation, even on Qwen3-family forks (e.g. Qwen3.6) that
 * ignore the {@code /no_think} directive.
 *
 * <p>Non-thinking models see only an extra (very short) assistant turn at the tail of the
 * conversation, which they typically treat as benign continuation context. So wiring this in
 * unconditionally for the lm-studio profile is safe.
 *
 * <p>Listeners ({@link ChatModelListener}) are inherited from the delegate, so the LangSmith
 * tracing still fires; the trace will include the prefill assistant turn, which is accurate — it
 * really is part of what was sent on the wire.
 */
public class NoThinkChatModel implements ChatModel {

  private static final ChatMessage PREFILL = AiMessage.from("<think>\n\n</think>\n");

  private final ChatModel delegate;

  public NoThinkChatModel(ChatModel delegate) {
    this.delegate = delegate;
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    List<ChatMessage> messages = new ArrayList<>(request.messages());
    messages.add(PREFILL);
    ChatRequest modified =
        ChatRequest.builder().messages(messages).parameters(request.parameters()).build();
    return delegate.chat(modified);
  }

  @Override
  public Set<Capability> supportedCapabilities() {
    return delegate.supportedCapabilities();
  }

  @Override
  public List<ChatModelListener> listeners() {
    return delegate.listeners();
  }

  @Override
  public ModelProvider provider() {
    return delegate.provider();
  }
}
