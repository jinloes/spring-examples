package com.jinloes.todo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinloes.todo.domain.Todo;
import com.jinloes.todo.repository.TodoRepository;
import com.jinloes.todo.service.TodoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(TodoController.class)
@Import(TodoService.class)
class TodoControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    TodoRepository todoRepository;

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private Todo todo(String id, String title, String description, boolean completed) {
        return Todo.builder()
                .id(id)
                .title(title)
                .description(description)
                .completed(completed)
                .createdAt(NOW)
                .build();
    }

    @Test
    void getAllTodos_returnsListOfTodos() {
        List<Todo> expected = List.of(
                todo("1", "Buy milk", "From the store", false),
                todo("2", "Walk dog", null, true)
        );
        when(todoRepository.findAll()).thenReturn(expected);

        var body = assertThat(mockMvc.get().uri("/api/todos"))
                .hasStatusOk()
                .bodyJson();

        body.extractingPath("$[0].id").asString().isEqualTo("1");
        body.extractingPath("$[0].title").asString().isEqualTo("Buy milk");
        body.extractingPath("$[0].completed").asBoolean().isFalse();
        body.extractingPath("$[1].id").asString().isEqualTo("2");
        body.extractingPath("$[1].title").asString().isEqualTo("Walk dog");
        body.extractingPath("$[1].completed").asBoolean().isTrue();
    }

    @Test
    void getAllTodos_returnsEmptyList() {
        when(todoRepository.findAll()).thenReturn(List.of());

        assertThat(mockMvc.get().uri("/api/todos"))
                .hasStatusOk()
                .bodyJson()
                .isEqualTo("[]");
    }

    @Test
    void getTodo_returnsExistingTodo() {
        Todo expected = todo("1", "Buy milk", "From the store", false);
        when(todoRepository.findById("1")).thenReturn(Optional.of(expected));

        var body = assertThat(mockMvc.get().uri("/api/todos/1"))
                .hasStatusOk()
                .bodyJson();

        body.extractingPath("$.id").asString().isEqualTo("1");
        body.extractingPath("$.title").asString().isEqualTo("Buy milk");
        body.extractingPath("$.description").asString().isEqualTo("From the store");
        body.extractingPath("$.completed").asBoolean().isFalse();
    }

    @Test
    void getTodo_returns404WhenNotFound() {
        when(todoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/api/todos/missing"))
                .hasStatus(404);
    }

    @Test
    void createTodo_returns201WithCreatedTodo() throws Exception {
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new TodoController.CreateTodoRequest("Buy milk", "From the store");

        var body = assertThat(mockMvc.post().uri("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .hasStatus(201)
                .bodyJson();

        body.extractingPath("$.id").asString().isNotEmpty();
        body.extractingPath("$.title").asString().isEqualTo("Buy milk");
        body.extractingPath("$.description").asString().isEqualTo("From the store");
        body.extractingPath("$.completed").asBoolean().isFalse();
    }

    @Test
    void updateTodo_returnsUpdatedTodo() throws Exception {
        when(todoRepository.findById("1")).thenReturn(Optional.of(todo("1", "Buy milk", "From the store", false)));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new TodoController.UpdateTodoRequest("Buy oat milk", "From the organic store");

        var body = assertThat(mockMvc.put().uri("/api/todos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .hasStatusOk()
                .bodyJson();

        body.extractingPath("$.id").asString().isEqualTo("1");
        body.extractingPath("$.title").asString().isEqualTo("Buy oat milk");
        body.extractingPath("$.description").asString().isEqualTo("From the organic store");
        body.extractingPath("$.completed").asBoolean().isFalse();
    }

    @Test
    void updateTodo_returns404WhenNotFound() throws Exception {
        when(todoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(mockMvc.put().uri("/api/todos/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TodoController.UpdateTodoRequest("Title", "Desc"))))
                .hasStatus(404);
    }

    @Test
    void completeTodo_returnsCompletedTodo() {
        when(todoRepository.findById("1")).thenReturn(Optional.of(todo("1", "Buy milk", "From the store", false)));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var body = assertThat(mockMvc.patch().uri("/api/todos/1/complete"))
                .hasStatusOk()
                .bodyJson();

        body.extractingPath("$.id").asString().isEqualTo("1");
        body.extractingPath("$.title").asString().isEqualTo("Buy milk");
        body.extractingPath("$.completed").asBoolean().isTrue();
    }

    @Test
    void completeTodo_returns404WhenNotFound() {
        when(todoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(mockMvc.patch().uri("/api/todos/missing/complete"))
                .hasStatus(404);
    }

    @Test
    void deleteTodo_returns204() {
        when(todoRepository.existsById("1")).thenReturn(true);

        assertThat(mockMvc.delete().uri("/api/todos/1"))
                .hasStatus(204);

        verify(todoRepository).deleteById("1");
    }

    @Test
    void deleteTodo_returns404WhenNotFound() {
        when(todoRepository.existsById("missing")).thenReturn(false);

        assertThat(mockMvc.delete().uri("/api/todos/missing"))
                .hasStatus(404);
    }
}