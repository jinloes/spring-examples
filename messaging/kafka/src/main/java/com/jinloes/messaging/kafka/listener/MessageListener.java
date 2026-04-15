package com.jinloes.messaging.kafka.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageListener {

  @KafkaListener(topics = "messages", groupId = "${spring.kafka.consumer.group-id}")
  public void listen(@Payload String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log.info("Received from topic '{}': {}", topic, message);
  }
}
