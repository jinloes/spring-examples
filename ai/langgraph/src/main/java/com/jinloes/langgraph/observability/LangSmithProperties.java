package com.jinloes.langgraph.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "langsmith")
public record LangSmithProperties(boolean enabled, String endpoint, String apiKey, String project) {

  public boolean active() {
    return enabled && apiKey != null && !apiKey.isBlank();
  }
}
