package com.jinloes.messaging.redis.messaging;

import com.jinloes.messaging.redis.model.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Receives user-targeted messages from the Redis {@code user-messages} channel and delivers them to
 * locally connected WebSocket clients.
 *
 * <h2>How distributed routing works</h2>
 *
 * <pre>
 * POST /api/messages/{username}  (hits any instance)
 *         │
 *         ▼
 * Redis pub/sub  ──────────────────────────────────────────────
 *         │                                                    │
 *         ▼                                                    ▼
 *    Instance A                                          Instance B
 *  forwardToUser(json)                               forwardToUser(json)
 *  simpUserRegistry.getUser("alice") → null          simpUserRegistry.getUser("alice") → SimpUser
 *  skip                                              convertAndSendToUser("alice", ...)
 *                                                          │
 *                                                          ▼
 *                                                  alice's browser receives message
 * </pre>
 *
 * <p>{@link SimpUserRegistry} is instance-local — it only knows about WebSocket sessions connected
 * to the current process. Publishing to Redis ensures every instance has the chance to check, so
 * the message is always delivered regardless of which instance the REST request landed on.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserMessageForwarder {

  private final SimpMessagingTemplate messagingTemplate;
  private final SimpUserRegistry simpUserRegistry;
  private final ObjectMapper objectMapper;

  /**
   * Called by {@link org.springframework.data.redis.listener.adapter.MessageListenerAdapter} when a
   * message arrives on the {@code user-messages} Redis channel.
   */
  public void forwardToUser(String json) {
    try {
      UserMessage userMessage = objectMapper.readValue(json, UserMessage.class);
      if (simpUserRegistry.getUser(userMessage.to()) != null) {
        // Target user has an active session on THIS instance — deliver locally
        log.info(
            "Delivering to user '{}' on this instance: {}", userMessage.to(), userMessage.body());
        messagingTemplate.convertAndSendToUser(
            userMessage.to(), "/queue/messages", userMessage.body());
      } else {
        // Not connected here — another instance will handle it (or the user is offline)
        log.debug("User '{}' not on this instance, skipping", userMessage.to());
      }
    } catch (Exception e) {
      log.error("Failed to parse user message envelope: {}", json, e);
    }
  }
}
