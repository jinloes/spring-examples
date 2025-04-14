package com.jinloes.loom_web;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class LoomHelloWorldController {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoomHelloWorldController.class);

  @GetMapping
  public ResponseEntity<String> hello() throws InterruptedException {
    LOGGER.info("Hello: " + Thread.currentThread());

    Thread.sleep(Duration.ofSeconds(10L).toMillis());

    return ResponseEntity.ok("hello");
  }
}
