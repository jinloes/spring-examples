package com.jinloes.messaging.redis.messaging;

import java.security.Principal;

/**
 * Minimal {@link Principal} that carries a username into the WebSocket session.
 *
 * <p>Created by {@link com.jinloes.messaging.redis.config.WebSocketConfig}'s handshake handler from
 * the {@code ?username=} query parameter. Spring stores this principal on the STOMP session so that
 * {@code SimpUserRegistry} and {@code convertAndSendToUser} can look up sessions by name.
 *
 * <p><strong>Demo use only</strong> — in production, derive the principal from a verified JWT token
 * or Spring Security authentication, never a plain query parameter.
 */
public record StompPrincipal(String name) implements Principal {
  @Override
  public String getName() {
    return name;
  }
}
