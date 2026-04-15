package com.jinloes.messaging.redis.config;

import com.jinloes.messaging.redis.messaging.RedisMessageForwarder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

  /** Topic name shared between the publisher (REST controller) and the subscriber (forwarder). */
  public static final String CHAT_CHANNEL = "chat";

  @Bean
  RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter, new PatternTopic(CHAT_CHANNEL));
    return container;
  }

  @Bean
  MessageListenerAdapter messageListenerAdapter(RedisMessageForwarder forwarder) {
    // Delegates to RedisMessageForwarder#forwardToWebSocket(String)
    return new MessageListenerAdapter(forwarder, "forwardToWebSocket");
  }
}
