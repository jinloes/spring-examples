package com.jinloes.langgraph.config;

import com.jinloes.langgraph.nodes.IntentClassifier;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * Fires a couple of throwaway chat calls once the application is fully up, but only when the {@code
 * lm-studio} profile is active. The first call against a freshly-loaded local model typically takes
 * 30–90 s while the server allocates KV-cache buffers, JIT-compiles GPU kernels, warms its
 * tokenizer, and primes its prompt cache. Doing it here moves that cost from the first real user
 * message to startup.
 *
 * <p>What each warmup step prepays:
 *
 * <ul>
 *   <li><b>Basic call ({@code chatModel.chat("warmup")})</b> — forces KV-cache buffer allocation,
 *       tokenizer init, GPU kernel JIT, and primes the Spring RestClient connection pool. Biggest
 *       single win.
 *   <li><b>Classifier call</b> — primes llama.cpp's prompt cache for the classifier's ~150-token
 *       system prompt. On the next real turn the server can skip prefill of the system tokens and
 *       only prefill the user's actual message (~5–30 tokens).
 * </ul>
 *
 * <p>This intentionally does NOT warm every agent (refund/technical/billing/clarify) because each
 * call adds ~5–30 s to startup and only the classifier runs on every turn. The agents still take
 * the cold-prompt hit on their first invocation, but only once each.
 */
@Slf4j
@Profile("lm-studio")
@Configuration
@RequiredArgsConstructor
public class LmStudioWarmup {

  private final ChatModel chatModel;
  private final IntentClassifier intentClassifier;

  @EventListener(ApplicationReadyEvent.class)
  public void warmup() {
    log.info(
        "LM Studio warmup starting — first user request would otherwise hit a cold KV cache "
            + "and HTTP connection pool, often costing 30–90s.");
    Instant start = Instant.now();

    try {
      // Cap output to 5 tokens. The point of the warmup is to exercise the server (allocate
      // KV-cache buffers, JIT GPU kernels, prime the HTTP connection pool), not to get a
      // useful answer. Reasoning models like Qwen3.6 still produce reasoning tokens here,
      // but NoThinkChatModel injects a `<think></think>` prefill that short-circuits that.
      ChatRequest req =
          ChatRequest.builder()
              .messages(UserMessage.from("hi"))
              .parameters(ChatRequestParameters.builder().maxOutputTokens(5).build())
              .build();
      chatModel.chat(req);
      log.info(
          "LM Studio basic warmup done in {} ms (KV cache + tokenizer + GPU kernels + connection)",
          Duration.between(start, Instant.now()).toMillis());
    } catch (Exception e) {
      log.warn("LM Studio basic warmup failed (continuing anyway): {}", e.getMessage());
      return;
    }

    Instant classifierStart = Instant.now();
    try {
      // memoryId "warmup-session" namespaces under "classifier:warmup-session" via AgentsConfig,
      // so this never collides with a real chat session.
      intentClassifier.classify("warmup-session", "warmup");
      log.info(
          "LM Studio classifier warmup done in {} ms (classifier system-prompt cached)",
          Duration.between(classifierStart, Instant.now()).toMillis());
    } catch (Exception e) {
      log.warn("LM Studio classifier warmup failed (continuing anyway): {}", e.getMessage());
    }

    log.info(
        "LM Studio total warmup time: {} ms — chat UI is now ready for fast first turn",
        Duration.between(start, Instant.now()).toMillis());
  }
}
