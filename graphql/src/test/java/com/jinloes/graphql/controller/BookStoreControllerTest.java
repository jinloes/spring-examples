package com.jinloes.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.jinloes.graphql.model.Author;
import com.jinloes.graphql.model.Book;
import com.jinloes.graphql.model.Review;
import com.jinloes.graphql.repository.AuthorRepository;
import com.jinloes.graphql.repository.BookRepository;
import com.jinloes.graphql.repository.ReviewRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * GraphQL slice tests — loads only the controller beans and the schema; repositories are mocked.
 *
 * <p>Run with: {@code ./gradlew :graphql:test}
 */
@GraphQlTest({BookController.class, AuthorController.class})
class BookStoreControllerTest {

  @Autowired GraphQlTester tester;

  @MockitoBean BookRepository bookRepository;
  @MockitoBean AuthorRepository authorRepository;
  @MockitoBean ReviewRepository reviewRepository;

  private final Author martinAuthor = new Author("a1", "Robert C. Martin", "Software engineer.");
  private final Book cleanCode = new Book("b1", "Clean Code", "a1");
  private final Book cleanArch = new Book("b2", "Clean Architecture", "a1");
  private final Review review1 = new Review("r1", "b1", 5, "Must-read.");
  private final Review review2 = new Review("r2", "b1", 4, "Great insights.");

  @BeforeEach
  void setUp() {
    // Default stubs used across multiple tests — override per-test where needed.
    given(bookRepository.findAll()).willReturn(List.of(cleanCode, cleanArch));
    given(authorRepository.findAllById(any())).willReturn(Map.of("a1", martinAuthor));
    given(reviewRepository.findByBookId("b1")).willReturn(List.of(review1, review2));
    given(reviewRepository.findByBookId("b2")).willReturn(List.of());
  }

  // ---------------------------------------------------------------------------
  // Books query
  // ---------------------------------------------------------------------------

  @Nested
  class BooksQuery {

    @Test
    void returnsAllBooksWithTitles() {
      tester
          .document("{ books { id title } }")
          .execute()
          .path("books[0].title")
          .entity(String.class)
          .isEqualTo("Clean Code")
          .path("books[1].title")
          .entity(String.class)
          .isEqualTo("Clean Architecture");
    }

    @Test
    void resolvesAuthorViaBatchMapping() {
      // Verifies @BatchMapping: one authorRepository call for all books, not one per book.
      tester
          .document("{ books { title author { name } } }")
          .execute()
          .path("books[0].author.name")
          .entity(String.class)
          .isEqualTo("Robert C. Martin");

      // Repository was called once for all books, not N times.
      then(authorRepository).should().findAllById(any());
    }

    @Test
    void resolvesReviewsAndAverageRating() {
      tester
          .document("{ books { title reviews { rating content } averageRating } }")
          .execute()
          .path("books[0].reviews")
          .entityList(Object.class)
          .hasSize(2)
          .path("books[0].averageRating")
          .entity(Double.class)
          .isEqualTo(4.5);
    }

    @Test
    void averageRatingIsZeroWhenNoReviews() {
      tester
          .document("{ books { title averageRating } }")
          .execute()
          .path("books[1].averageRating") // cleanArch has no reviews
          .entity(Double.class)
          .isEqualTo(0.0);
    }
  }

  // ---------------------------------------------------------------------------
  // Book query (by ID)
  // ---------------------------------------------------------------------------

  @Nested
  class BookByIdQuery {

    @Test
    void returnsBookWhenFound() {
      given(bookRepository.findById("b1")).willReturn(Optional.of(cleanCode));

      tester
          .document("{ book(id: \"b1\") { title author { name } } }")
          .execute()
          .path("book.title")
          .entity(String.class)
          .isEqualTo("Clean Code")
          .path("book.author.name")
          .entity(String.class)
          .isEqualTo("Robert C. Martin");
    }

    @Test
    void returnsNullWhenNotFound() {
      given(bookRepository.findById(anyString())).willReturn(Optional.empty());

      tester.document("{ book(id: \"unknown\") { title } }").execute().path("book").valueIsNull();
    }
  }

  // ---------------------------------------------------------------------------
  // Authors query
  // ---------------------------------------------------------------------------

  @Nested
  class AuthorsQuery {

    @Test
    void returnsAllAuthorsWithTheirBooks() {
      given(authorRepository.findAll()).willReturn(List.of(martinAuthor));

      tester
          .document("{ authors { name books { title } } }")
          .execute()
          .path("authors[0].name")
          .entity(String.class)
          .isEqualTo("Robert C. Martin")
          .path("authors[0].books")
          .entityList(Object.class)
          .hasSize(2);
    }
  }

  // ---------------------------------------------------------------------------
  // Mutations
  // ---------------------------------------------------------------------------

  @Nested
  class Mutations {

    @Test
    void createBookReturnsNewBook() {
      Book newBook = new Book("b-new", "Refactoring", "a1");
      given(bookRepository.save(any())).willReturn(newBook);
      given(authorRepository.findAllById(any())).willReturn(Map.of("a1", martinAuthor));
      given(reviewRepository.findByBookId("b-new")).willReturn(List.of());

      tester
          .document(
              """
              mutation {
                createBook(input: { title: "Refactoring", authorId: "a1" }) {
                  id title author { name } averageRating
                }
              }
              """)
          .execute()
          .path("createBook.title")
          .entity(String.class)
          .isEqualTo("Refactoring")
          .path("createBook.author.name")
          .entity(String.class)
          .isEqualTo("Robert C. Martin")
          .path("createBook.averageRating")
          .entity(Double.class)
          .isEqualTo(0.0);
    }

    @Test
    void createAuthorReturnsNewAuthor() {
      Author newAuthor = new Author("a-new", "Martin Fowler", "Refactoring author.");
      given(authorRepository.save(any())).willReturn(newAuthor);

      tester
          .document(
              """
              mutation {
                createAuthor(input: { name: "Martin Fowler", bio: "Refactoring author." }) {
                  id name bio
                }
              }
              """)
          .execute()
          .path("createAuthor.name")
          .entity(String.class)
          .isEqualTo("Martin Fowler")
          .path("createAuthor.bio")
          .entity(String.class)
          .isEqualTo("Refactoring author.");
    }

    @Test
    void addReviewReturnsNewReview() {
      Review newReview = new Review("r-new", "b1", 5, "Excellent.");
      given(reviewRepository.save(any())).willReturn(newReview);

      tester
          .document(
              """
              mutation {
                addReview(bookId: "b1", input: { rating: 5, content: "Excellent." }) {
                  id rating content
                }
              }
              """)
          .execute()
          .path("addReview.rating")
          .entity(Integer.class)
          .isEqualTo(5)
          .path("addReview.content")
          .entity(String.class)
          .isEqualTo("Excellent.");
    }

    @Test
    void deleteBookReturnsTrueWhenFound() {
      given(bookRepository.deleteById("b1")).willReturn(true);

      tester
          .document("mutation { deleteBook(id: \"b1\") }")
          .execute()
          .path("deleteBook")
          .entity(Boolean.class)
          .isEqualTo(true);
    }

    @Test
    void deleteBookReturnsFalseWhenNotFound() {
      given(bookRepository.deleteById(anyString())).willReturn(false);

      tester
          .document("mutation { deleteBook(id: \"unknown\") }")
          .execute()
          .path("deleteBook")
          .entity(Boolean.class)
          .isEqualTo(false);
    }
  }

  // ---------------------------------------------------------------------------
  // Schema integrity
  // ---------------------------------------------------------------------------

  @Nested
  class SchemaIntegrity {

    @Test
    void bookFieldsAllResolve() {
      given(bookRepository.findById("b1")).willReturn(Optional.of(cleanCode));

      tester
          .document(
              """
              {
                book(id: "b1") {
                  id title
                  author { id name bio }
                  reviews { id rating content }
                  averageRating
                }
              }
              """)
          .execute()
          .errors()
          .verify();
    }

    @Test
    void assertBookListIsNotEmpty() {
      List<String> titles =
          tester
              .document("{ books { title } }")
              .execute()
              .path("books[*].title")
              .entityList(String.class)
              .get();

      assertThat(titles).containsExactly("Clean Code", "Clean Architecture");
    }
  }
}
