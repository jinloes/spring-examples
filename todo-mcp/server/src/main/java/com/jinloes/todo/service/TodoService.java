package com.jinloes.todo.service;

import com.jinloes.todo.domain.Todo;
import com.jinloes.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoRepository repository;

    @Tool(description = "Create a new todo item with a title and optional description")
    public Todo createTodo(
            @ToolParam(description = "Title of the todo item") String title,
            @ToolParam(description = "Optional description of the todo item") String description) {
        Todo todo = Todo.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .description(description)
                .completed(false)
                .createdAt(Instant.now())
                .build();
        return repository.save(todo);
    }

    @Tool(description = "Get all todo items")
    public List<Todo> getAllTodos() {
        return repository.findAll();
    }

    @Tool(description = "Get a todo item by its ID")
    public Todo getTodoById(@ToolParam(description = "The ID of the todo item") String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Todo not found: " + id));
    }

    @Tool(description = "Update the title and description of an existing todo item")
    public Todo updateTodo(
            @ToolParam(description = "The ID of the todo item") String id,
            @ToolParam(description = "New title") String title,
            @ToolParam(description = "New description") String description) {
        Todo todo = getTodoById(id);
        todo.setTitle(title);
        todo.setDescription(description);
        return repository.save(todo);
    }

    @Tool(description = "Mark a todo item as completed")
    public Todo completeTodo(@ToolParam(description = "The ID of the todo item") String id) {
        Todo todo = getTodoById(id);
        todo.setCompleted(true);
        return repository.save(todo);
    }

    @Tool(description = "Delete a todo item by its ID")
    public void deleteTodo(@ToolParam(description = "The ID of the todo item") String id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Todo not found: " + id);
        }
        repository.deleteById(id);
    }
}