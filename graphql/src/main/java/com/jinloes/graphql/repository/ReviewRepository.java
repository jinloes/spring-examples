package com.jinloes.graphql.repository;

import com.jinloes.graphql.model.Review;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {

  private final Map<String, Review> store = new LinkedHashMap<>();

  public ReviewRepository() {
    save(new Review("r1", "book-1", 5, "A must-read — transformed how I write code."));
    save(new Review("r2", "book-1", 4, "Excellent principles, slightly dated Java examples."));
    save(new Review("r3", "book-2", 5, "Changed how I think about software craftsmanship."));
    save(new Review("r4", "book-2", 5, "Timeless advice that still applies decades later."));
    save(new Review("r5", "book-3", 5, "The definitive Java reference — re-read it every year."));
    save(new Review("r6", "book-4", 4, "Great companion to Clean Code."));
  }

  public List<Review> findByBookId(String bookId) {
    return store.values().stream().filter(r -> r.bookId().equals(bookId)).toList();
  }

  public Review save(Review review) {
    store.put(review.id(), review);
    return review;
  }
}
