package com.jinloes.todo.service;

import com.jinloes.todo.domain.Todo;
import com.jinloes.todo.repository.TodoRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

  private final TodoRepository repository;

  @Tool(description = "Create a new todo item with a title and optional description")
  public Todo createTodo(
      @ToolParam(description = "Title of the todo item") String title,
      @ToolParam(description = "Optional description of the todo item") String description) {
    Todo todo =
        Todo.builder()
            .id(UUID.randomUUID().toString())
            .title(title)
            .description(description)
            .completed(false)
            .createdAt(Instant.now())
            .build();
    Todo saved = repository.save(todo);
    log.info("Created todo id=%s title='%s'".formatted(saved.getId(), saved.getTitle()));
    return saved;
  }

  @Tool(description = "Get all todo items")
  public List<Todo> getAllTodos() {
    List<Todo> todos = repository.findAll();
    log.info("Retrieved %d todos".formatted(todos.size()));
    return todos;
  }

  @Tool(description = "Get a todo item by its ID")
  public Todo getTodoById(@ToolParam(description = "The ID of the todo item") String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> {
              log.error("Todo not found: %s".formatted(id));
              return new NoSuchElementException("Todo not found: " + id);
            });
  }

  @Tool(description = "Update the title and description of an existing todo item")
  public Todo updateTodo(
      @ToolParam(description = "The ID of the todo item") String id,
      @ToolParam(description = "New title") String title,
      @ToolParam(description = "New description") String description) {
    Todo todo = getTodoById(id);
    todo.setTitle(title);
    todo.setDescription(description);
    Todo saved = repository.save(todo);
    log.info("Updated todo id=%s title='%s'".formatted(saved.getId(), saved.getTitle()));
    return saved;
  }

  @Tool(description = "Mark a todo item as completed")
  public Todo completeTodo(@ToolParam(description = "The ID of the todo item") String id) {
    Todo todo = getTodoById(id);
    todo.setCompleted(true);
    Todo saved = repository.save(todo);
    log.info("Completed todo id=%s".formatted(saved.getId()));
    return saved;
  }

  @Tool(description = "Delete a todo item by its ID")
  public void deleteTodo(@ToolParam(description = "The ID of the todo item") String id) {
    if (!repository.existsById(id)) {
      log.error("Delete failed — todo not found: %s".formatted(id));
      throw new NoSuchElementException("Todo not found: " + id);
    }
    repository.deleteById(id);
    log.info("Deleted todo id=%s".formatted(id));
  }
}
