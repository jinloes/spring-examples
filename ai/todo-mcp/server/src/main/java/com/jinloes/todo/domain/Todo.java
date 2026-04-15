package com.jinloes.todo.domain;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Todo {
  private String id;
  private String title;
  private String description;
  private boolean completed;
  private Instant createdAt;
}
