package com.jinloes.messaging.redis.web;

import com.jinloes.messaging.redis.config.RedisConfig;
import com.jinloes.messaging.redis.model.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Broadcasts a message to all connected WebSocket clients via the Redis {@code chat} channel.
   * Every app instance receives it and forwards it to their local subscribers.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void broadcast(@RequestBody String message) {
    redisTemplate.convertAndSend(RedisConfig.CHAT_CHANNEL, message);
    log.info("Broadcast to Redis channel '{}': {}", RedisConfig.CHAT_CHANNEL, message);
  }

  /**
   * Sends a message to a specific user via the Redis {@code user-messages} channel.
   *
   * <p>The message envelope is published to Redis so every instance receives it. Each instance
   * checks its local {@link org.springframework.messaging.simp.user.SimpUserRegistry} — only the
   * instance where {@code username} is connected delivers the message. Other instances silently
   * skip it. This means delivery works correctly regardless of which instance handles this request.
   */
  @PostMapping("/{username}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void sendToUser(@PathVariable String username, @RequestBody String message)
      throws JacksonException {
    String envelope = objectMapper.writeValueAsString(new UserMessage(username, message));
    redisTemplate.convertAndSend(RedisConfig.USER_MESSAGES_CHANNEL, envelope);
    log.info("Published user message to Redis for '{}': {}", username, message);
  }
}
