package com.jinloes.todo.web;

import com.jinloes.todo.domain.Todo;
import com.jinloes.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public List<Todo> getAllTodos() {
        return todoService.getAllTodos();
    }

    @GetMapping("/{id}")
    public Todo getTodo(@PathVariable String id) {
        return todoService.getTodoById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo createTodo(@RequestBody CreateTodoRequest request) {
        return todoService.createTodo(request.title(), request.description());
    }

    @PutMapping("/{id}")
    public Todo updateTodo(@PathVariable String id, @RequestBody UpdateTodoRequest request) {
        return todoService.updateTodo(id, request.title(), request.description());
    }

    @PatchMapping("/{id}/complete")
    public Todo completeTodo(@PathVariable String id) {
        return todoService.completeTodo(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTodo(@PathVariable String id) {
        todoService.deleteTodo(id);
    }

    public record CreateTodoRequest(String title, String description) {}

    public record UpdateTodoRequest(String title, String description) {}
}