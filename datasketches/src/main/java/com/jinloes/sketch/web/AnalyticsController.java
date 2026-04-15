package com.jinloes.sketch.web;

import com.jinloes.sketch.model.AnalyticsSummary;
import com.jinloes.sketch.model.Event;
import com.jinloes.sketch.model.PageFrequency;
import com.jinloes.sketch.model.ResponseTimeStats;
import com.jinloes.sketch.model.UniqueEstimate;
import com.jinloes.sketch.service.SketchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

  private static final int DEFAULT_POPULAR_PAGES_LIMIT = 10;

  private final SketchService sketchService;

  /** Records an analytics event. Null fields are ignored. */
  @PostMapping("/events")
  public ResponseEntity<Void> recordEvent(@RequestBody Event event) {
    sketchService.record(event);
    return ResponseEntity.accepted().build();
  }

  /** Full analytics snapshot across all four sketches. */
  @GetMapping("/analytics")
  public ResponseEntity<AnalyticsSummary> summary(
      @RequestParam(defaultValue = "" + DEFAULT_POPULAR_PAGES_LIMIT) int limit) {
    return ResponseEntity.ok(sketchService.summary(limit));
  }

  /**
   * Unique visitor count estimated by the <b>Theta sketch</b>, including 95% confidence bounds.
   *
   * <p>Theta sketches are ideal when you need set operations (e.g. union two daily sketches to get
   * a weekly unique count without replaying the raw events).
   */
  @GetMapping("/analytics/unique-visitors")
  public ResponseEntity<UniqueEstimate> uniqueVisitors() {
    return ResponseEntity.ok(sketchService.uniqueVisitors());
  }

  /**
   * Unique page count estimated by the <b>HLL sketch</b>, including 95% confidence bounds.
   *
   * <p>HLL uses fixed memory (lgK=12 → ~5 KB) regardless of cardinality, making it cheaper than
   * Theta for pure counting when set operations are not needed.
   */
  @GetMapping("/analytics/unique-pages")
  public ResponseEntity<UniqueEstimate> uniquePages() {
    return ResponseEntity.ok(sketchService.uniquePages());
  }

  /**
   * Response time percentiles (p50/p95/p99) from the <b>Quantiles sketch</b>.
   *
   * <p>The sketch answers percentile queries over arbitrary streams in O(log N) space — no need to
   * store all response times.
   */
  @GetMapping("/analytics/response-times")
  public ResponseEntity<ResponseTimeStats> responseTimes() {
    return ResponseEntity.ok(sketchService.responseTimes());
  }

  /**
   * Top-K most visited pages from the <b>Frequency sketch</b>.
   *
   * <p>Uses the space-saving algorithm: pages appearing frequently enough are guaranteed to appear
   * in the result (no false negatives above the error threshold).
   */
  @GetMapping("/analytics/popular-pages")
  public ResponseEntity<List<PageFrequency>> popularPages(
      @RequestParam(defaultValue = "" + DEFAULT_POPULAR_PAGES_LIMIT) int limit) {
    return ResponseEntity.ok(sketchService.popularPages(limit));
  }

  /** Resets all sketches, discarding all recorded events. */
  @DeleteMapping("/analytics")
  public ResponseEntity<Void> reset() {
    sketchService.reset();
    return ResponseEntity.noContent().build();
  }
}
