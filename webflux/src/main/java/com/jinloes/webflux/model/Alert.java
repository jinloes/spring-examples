package com.jinloes.webflux.model;

import java.util.UUID;

public record Alert(String id, String message, Severity severity) {
  public Alert withId() {
    return new Alert(UUID.randomUUID().toString(), message, severity);
  }
}
