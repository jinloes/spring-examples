package com.jinloes.spring_examples.elasticsearch;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AlarmNotFoundException extends RuntimeException {
  public AlarmNotFoundException(String id) {
    super("Alarm not found: " + id);
  }
}
