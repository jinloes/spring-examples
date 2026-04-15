package com.jinloes.todo.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class Todo {
    private String id;
    private String title;
    private String description;
    private boolean completed;
    private Instant createdAt;
}