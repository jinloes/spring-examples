package com.jinloes.messaging.redis.web;

import com.jinloes.messaging.redis.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

  private final StringRedisTemplate redisTemplate;

  /**
   * Publishes a message to the Redis chat channel. All app instances subscribed to the channel will
   * receive it and forward it to their connected WebSocket clients.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publish(@RequestBody String message) {
    redisTemplate.convertAndSend(RedisConfig.CHAT_CHANNEL, message);
    log.info("Published to Redis channel '{}': {}", RedisConfig.CHAT_CHANNEL, message);
  }
}
