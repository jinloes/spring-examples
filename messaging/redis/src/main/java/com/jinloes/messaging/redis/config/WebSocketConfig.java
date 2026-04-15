package com.jinloes.messaging.redis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures STOMP over WebSocket with an in-memory broker.
 *
 * <p>Redis is NOT used as the STOMP broker relay here — Spring's WebSocket support only relays to
 * full STOMP brokers (RabbitMQ, ActiveMQ). Instead, Redis pub/sub is used as the cross-instance
 * fan-out layer: messages published to Redis are received by {@link
 * com.jinloes.messaging.redis.messaging.RedisMessageForwarder}, which then pushes them to connected
 * WebSocket clients via {@code SimpMessagingTemplate}. This pattern works correctly in
 * multi-instance deployments where each instance maintains its own WebSocket connections.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // In-memory broker handles /topic/* destinations
    registry.enableSimpleBroker("/topic");
    // Prefix for client-to-server @MessageMapping methods
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // WebSocket endpoint with SockJS fallback for older browsers
    registry.addEndpoint("/ws").withSockJS();
  }
}
