package com.jinloes.graphql.repository;

import com.jinloes.graphql.model.Book;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class BookRepository {

  private final Map<String, Book> store = new LinkedHashMap<>();

  public BookRepository() {
    save(new Book("book-1", "Clean Code", "author-1"));
    save(new Book("book-2", "The Pragmatic Programmer", "author-2"));
    save(new Book("book-3", "Effective Java", "author-3"));
    save(new Book("book-4", "Clean Architecture", "author-1"));
  }

  public List<Book> findAll() {
    return List.copyOf(store.values());
  }

  public Optional<Book> findById(String id) {
    return Optional.ofNullable(store.get(id));
  }

  public List<Book> findByAuthorId(String authorId) {
    return store.values().stream().filter(b -> b.authorId().equals(authorId)).toList();
  }

  public Book save(Book book) {
    store.put(book.id(), book);
    return book;
  }

  public boolean deleteById(String id) {
    return store.remove(id) != null;
  }
}
