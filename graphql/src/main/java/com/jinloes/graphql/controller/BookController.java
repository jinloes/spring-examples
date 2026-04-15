package com.jinloes.graphql.controller;

import com.jinloes.graphql.model.AddReviewInput;
import com.jinloes.graphql.model.Author;
import com.jinloes.graphql.model.Book;
import com.jinloes.graphql.model.CreateBookInput;
import com.jinloes.graphql.model.Review;
import com.jinloes.graphql.repository.AuthorRepository;
import com.jinloes.graphql.repository.BookRepository;
import com.jinloes.graphql.repository.ReviewRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * Handles all queries and mutations for {@link Book}, plus field resolvers for the {@code Book}
 * GraphQL type.
 *
 * <p>The class-level {@code @SchemaMapping(typeName = "Book")} sets the default type for field
 * resolver methods. {@code @QueryMapping} and {@code @MutationMapping} override this to {@code
 * Query} and {@code Mutation} respectively.
 */
@Controller
@SchemaMapping(typeName = "Book")
@RequiredArgsConstructor
public class BookController {

  private final BookRepository bookRepository;
  private final AuthorRepository authorRepository;
  private final ReviewRepository reviewRepository;

  // ---------------------------------------------------------------------------
  // Queries
  // ---------------------------------------------------------------------------

  @QueryMapping
  public List<Book> books() {
    return bookRepository.findAll();
  }

  @QueryMapping
  public Optional<Book> book(@Argument String id) {
    return bookRepository.findById(id);
  }

  // ---------------------------------------------------------------------------
  // Mutations
  // ---------------------------------------------------------------------------

  @MutationMapping
  public Book createBook(@Argument CreateBookInput input) {
    Book book = new Book(UUID.randomUUID().toString(), input.title(), input.authorId());
    return bookRepository.save(book);
  }

  @MutationMapping
  public boolean deleteBook(@Argument String id) {
    return bookRepository.deleteById(id);
  }

  @MutationMapping
  public Review addReview(@Argument String bookId, @Argument AddReviewInput input) {
    Review review =
        new Review(UUID.randomUUID().toString(), bookId, input.rating(), input.content());
    return reviewRepository.save(review);
  }

  // ---------------------------------------------------------------------------
  // Field resolvers for the Book type
  // ---------------------------------------------------------------------------

  /**
   * Resolves {@code Book.author} for a batch of books in a single call, preventing the N+1 query
   * problem.
   *
   * <p>Without batching, resolving {@code author} for N books would trigger N separate repository
   * lookups. {@code @BatchMapping} collects all books needing their author resolved, then calls
   * this method once with the full list. Spring for GraphQL uses the returned map to assign each
   * book its author.
   */
  @BatchMapping
  public Map<Book, Author> author(List<Book> books) {
    Set<String> authorIds = books.stream().map(Book::authorId).collect(Collectors.toSet());
    Map<String, Author> byId = authorRepository.findAllById(authorIds);
    return books.stream().collect(Collectors.toMap(b -> b, b -> byId.get(b.authorId())));
  }

  /** Resolves {@code Book.reviews} — the list of reviews for a single book. */
  @SchemaMapping
  public List<Review> reviews(Book book) {
    return reviewRepository.findByBookId(book.id());
  }

  /**
   * Resolves {@code Book.averageRating} as a computed field — not stored, derived from reviews.
   * Returns {@code 0.0} when the book has no reviews yet.
   */
  @SchemaMapping
  public double averageRating(Book book) {
    return reviewRepository.findByBookId(book.id()).stream()
        .mapToInt(Review::rating)
        .average()
        .orElse(0.0);
  }
}
