package com.jinloes.graphql.controller;

import com.jinloes.graphql.model.Author;
import com.jinloes.graphql.model.Book;
import com.jinloes.graphql.model.CreateAuthorInput;
import com.jinloes.graphql.repository.AuthorRepository;
import com.jinloes.graphql.repository.BookRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Handles all queries and mutations for {@link Author}, plus field resolvers for the {@code Author}
 * GraphQL type.
 */
@Controller
@SchemaMapping(typeName = "Author")
@RequiredArgsConstructor
public class AuthorController {

  private final AuthorRepository authorRepository;
  private final BookRepository bookRepository;

  // ---------------------------------------------------------------------------
  // Queries
  // ---------------------------------------------------------------------------

  @QueryMapping
  public List<Author> authors() {
    return authorRepository.findAll();
  }

  @QueryMapping
  public Optional<Author> author(@Argument String id) {
    return authorRepository.findById(id);
  }

  // ---------------------------------------------------------------------------
  // Mutations
  // ---------------------------------------------------------------------------

  @MutationMapping
  public Author createAuthor(@Argument CreateAuthorInput input) {
    Author author = new Author(UUID.randomUUID().toString(), input.name(), input.bio());
    return authorRepository.save(author);
  }

  // ---------------------------------------------------------------------------
  // Field resolvers for the Author type
  // ---------------------------------------------------------------------------

  /**
   * Resolves {@code Author.books} for a batch of authors in a single call — same N+1 prevention
   * pattern as {@link BookController#author}.
   *
   * <p>Groups all books by {@code authorId}, then maps each author to its book list. Authors with
   * no books receive an empty list.
   */
  @BatchMapping
  public Map<Author, List<Book>> books(List<Author> authors) {
    Map<String, List<Book>> byAuthorId =
        bookRepository.findAll().stream().collect(Collectors.groupingBy(Book::authorId));
    return authors.stream()
        .collect(Collectors.toMap(a -> a, a -> byAuthorId.getOrDefault(a.id(), List.of())));
  }
}
