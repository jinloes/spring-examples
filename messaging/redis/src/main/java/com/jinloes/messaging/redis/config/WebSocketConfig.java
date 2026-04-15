package com.jinloes.messaging.redis.config;

import com.jinloes.messaging.redis.messaging.StompPrincipal;
import java.security.Principal;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Configures STOMP over WebSocket with an in-memory broker.
 *
 * <h2>Principal assignment</h2>
 *
 * The custom {@link DefaultHandshakeHandler} reads a {@code ?username=} query parameter during the
 * WebSocket handshake and wraps it in a {@link StompPrincipal}. Spring stores this principal on the
 * STOMP session, enabling {@code SimpUserRegistry} lookups and {@code convertAndSendToUser}
 * routing.
 *
 * <p><strong>Demo use only</strong> — production code should derive the principal from a verified
 * credential (JWT, Spring Security authentication), not a plain query parameter.
 *
 * <h2>Broker destinations</h2>
 *
 * <ul>
 *   <li>{@code /topic/*} — broadcast; all subscribers receive the message
 *   <li>{@code /queue/*} — user-specific; Spring rewrites {@code /user/queue/messages} to a
 *       per-session destination so only the target user receives it
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setHandshakeHandler(
            new DefaultHandshakeHandler() {
              @Override
              protected Principal determineUser(
                  ServerHttpRequest request,
                  WebSocketHandler wsHandler,
                  Map<String, Object> attributes) {
                // Extract username from ?username= query parameter
                String query = request.getURI().getQuery();
                if (query != null) {
                  for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "username".equals(kv[0])) {
                      String username = kv[1].trim();
                      if (StringUtils.hasText(username)) {
                        return new StompPrincipal(username);
                      }
                    }
                  }
                }
                return null;
              }
            })
        .withSockJS();
  }
}
