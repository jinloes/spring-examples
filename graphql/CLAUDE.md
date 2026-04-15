# graphql

Spring Boot + Spring for GraphQL bookstore API demonstrating the core GraphQL patterns.

## How to Run

```bash
./gradlew :graphql:bootRun
```

- GraphiQL playground: http://localhost:8080/graphiql
- Schema introspection: printed to stdout on startup

## Domain

A bookstore with three types and bidirectional relationships:

```
Author ──< Book ──< Review
```

- **Author** — id, name, bio, books (list)
- **Book** — id, title, author, reviews (list), averageRating (computed)
- **Review** — id, rating, content

## Schema Overview

```graphql
type Query {
    books: [Book!]!
    book(id: ID!): Book        # nullable — returns null when not found
    authors: [Author!]!
    author(id: ID!): Author
}

type Mutation {
    createBook(input: CreateBookInput!): Book!
    createAuthor(input: CreateAuthorInput!): Author!
    addReview(bookId: ID!, input: AddReviewInput!): Review!
    deleteBook(id: ID!): Boolean!
}
```

## Key Patterns Demonstrated

### `@QueryMapping` / `@MutationMapping`
Standard query and mutation handlers. Map to `Query.<methodName>` and `Mutation.<methodName>` respectively.

### `@BatchMapping` — N+1 prevention
Without batching, resolving `books { author { name } }` would trigger one `authorRepository` call
per book. `@BatchMapping` collects all books needing their author resolved, then calls the method
once with the full list:

```java
@BatchMapping
public Map<Book, Author> author(List<Book> books) {
    Set<String> ids = books.stream().map(Book::authorId).collect(toSet());
    Map<String, Author> byId = authorRepository.findAllById(ids);
    return books.stream().collect(toMap(b -> b, b -> byId.get(b.authorId())));
}
```

Spring for GraphQL uses the returned map to assign each book its author — one repository call
regardless of how many books are in the list.

### `@SchemaMapping` — computed fields
Fields that don't exist in the stored model are resolved on demand:

```java
@SchemaMapping
public double averageRating(Book book) {
    return reviewRepository.findByBookId(book.id()).stream()
        .mapToInt(Review::rating).average().orElse(0.0);
}
```

`averageRating` is not stored anywhere — it's computed from reviews at query time.

### Class-level `@SchemaMapping`
Both controllers are annotated with `@SchemaMapping(typeName = "Book")` /
`@SchemaMapping(typeName = "Author")` at the class level. This sets the default type for
`@BatchMapping` and `@SchemaMapping` methods, so the field name is inferred from the method name
without repeating `typeName` on every method. `@QueryMapping` and `@MutationMapping` override
this to `Query` and `Mutation` respectively.

## Example Queries

### Fetch all books with author and rating
```graphql
{
  books {
    title
    author { name }
    averageRating
    reviews { rating content }
  }
}
```

### Fetch a single book
```graphql
{
  book(id: "book-1") {
    title
    author { name bio }
  }
}
```

### Fetch all authors with their books
```graphql
{
  authors {
    name
    books { title averageRating }
  }
}
```

### Create a book
```graphql
mutation {
  createBook(input: { title: "Refactoring", authorId: "author-1" }) {
    id title author { name }
  }
}
```

### Add a review
```graphql
mutation {
  addReview(bookId: "book-1", input: { rating: 5, content: "Excellent." }) {
    id rating content
  }
}
```

### Delete a book
```graphql
mutation {
  deleteBook(id: "book-1")
}
```

## Tests

Uses `@GraphQlTest` (GraphQL slice test) — loads only the controllers and schema; repositories
are replaced with Mockito mocks. No server or full application context needed.

```bash
./gradlew :graphql:test
```
