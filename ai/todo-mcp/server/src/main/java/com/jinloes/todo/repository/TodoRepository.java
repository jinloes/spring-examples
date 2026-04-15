package com.jinloes.todo.repository;

import com.jinloes.todo.domain.Todo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class TodoRepository {

  private final Map<String, Todo> store = new ConcurrentHashMap<>();

  public Todo save(Todo todo) {
    store.put(todo.getId(), todo);
    return todo;
  }

  public Optional<Todo> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  public List<Todo> findAll() {
    return new ArrayList<>(store.values());
  }

  public void deleteById(String id) {
    store.remove(id);
  }

  public boolean existsById(String id) {
    return store.containsKey(id);
  }
}
