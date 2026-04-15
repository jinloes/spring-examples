package com.jinloes.messaging.redis.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges Redis pub/sub to the WebSocket broker.
 *
 * <p>Called by Spring's {@link
 * org.springframework.data.redis.listener.adapter.MessageListenerAdapter} when a message arrives on
 * the Redis chat channel. Forwards it to all WebSocket clients subscribed to {@code
 * /topic/messages} via the in-memory STOMP broker.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageForwarder {

  private final SimpMessagingTemplate messagingTemplate;

  public void forwardToWebSocket(String message) {
    log.info("Forwarding from Redis to /topic/messages: {}", message);
    messagingTemplate.convertAndSend("/topic/messages", message);
  }
}
