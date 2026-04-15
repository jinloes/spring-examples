package com.jinloes.messaging.kafka.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

  private final KafkaTemplate<String, String> kafkaTemplate;

  @PostMapping("/{topic}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publish(@PathVariable String topic, @RequestBody String message) {
    kafkaTemplate.send(topic, message);
    log.info("Published to topic '{}': {}", topic, message);
  }
}
