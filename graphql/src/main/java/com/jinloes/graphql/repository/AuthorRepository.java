package com.jinloes.graphql.repository;

import com.jinloes.graphql.model.Author;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class AuthorRepository {

  private final Map<String, Author> store = new LinkedHashMap<>();

  public AuthorRepository() {
    save(new Author("author-1", "Robert C. Martin", "Software engineer and author of Clean Code."));
    save(
        new Author(
            "author-2", "Andrew Hunt & David Thomas", "Authors of The Pragmatic Programmer."));
    save(
        new Author("author-3", "Joshua Bloch", "Java platform lead and author of Effective Java."));
  }

  public List<Author> findAll() {
    return List.copyOf(store.values());
  }

  public Optional<Author> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  /** Batch lookup used by {@code @BatchMapping} to avoid N+1 queries. */
  public Map<String, Author> findAllById(Collection<String> ids) {
    return ids.stream().filter(store::containsKey).collect(Collectors.toMap(id -> id, store::get));
  }

  public Author save(Author author) {
    store.put(author.id(), author);
    return author;
  }
}
