package com.jinloes.messaging.redis.config;

import com.jinloes.messaging.redis.messaging.RedisMessageForwarder;
import com.jinloes.messaging.redis.messaging.UserMessageForwarder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

  /** Broadcast channel — message goes to every connected WebSocket client. */
  public static final String CHAT_CHANNEL = "chat";

  /**
   * User-targeted channel — message envelope contains the recipient username. Each instance checks
   * its local {@link org.springframework.messaging.simp.user.SimpUserRegistry} and delivers only if
   * the recipient is connected to that instance.
   */
  public static final String USER_MESSAGES_CHANNEL = "user-messages";

  @Bean
  RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      MessageListenerAdapter broadcastListenerAdapter,
      MessageListenerAdapter userListenerAdapter) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(broadcastListenerAdapter, new PatternTopic(CHAT_CHANNEL));
    container.addMessageListener(userListenerAdapter, new PatternTopic(USER_MESSAGES_CHANNEL));
    return container;
  }

  @Bean
  MessageListenerAdapter broadcastListenerAdapter(RedisMessageForwarder forwarder) {
    return new MessageListenerAdapter(forwarder, "forwardToWebSocket");
  }

  @Bean
  MessageListenerAdapter userListenerAdapter(UserMessageForwarder forwarder) {
    return new MessageListenerAdapter(forwarder, "forwardToUser");
  }
}
