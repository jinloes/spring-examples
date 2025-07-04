package com.jinloes.rate_limiting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RateLimitingApplication {
  public static void main(String[] args) {
    SpringApplication.run(RateLimitingApplication.class, args);
  }
}
