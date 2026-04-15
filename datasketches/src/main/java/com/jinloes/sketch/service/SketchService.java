package com.jinloes.sketch.service;

import com.jinloes.sketch.model.AnalyticsSummary;
import com.jinloes.sketch.model.Event;
import com.jinloes.sketch.model.PageFrequency;
import com.jinloes.sketch.model.ResponseTimeStats;
import com.jinloes.sketch.model.UniqueEstimate;
import java.util.Arrays;
import java.util.List;
import org.apache.datasketches.frequencies.ErrorType;
import org.apache.datasketches.frequencies.ItemsSketch;
import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.quantiles.DoublesSketch;
import org.apache.datasketches.quantiles.UpdateDoublesSketch;
import org.apache.datasketches.theta.UpdateSketch;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Maintains four in-memory sketches and exposes approximate analytics queries.
 *
 * <h2>Sketches in use</h2>
 *
 * <ul>
 *   <li><b>Theta sketch</b> ({@link UpdateSketch}) — unique visitor count with confidence
 *       intervals. Supports set operations (union/intersection) across time windows.
 *   <li><b>HLL sketch</b> ({@link HllSketch}) — unique page count. More memory-efficient than Theta
 *       at the cost of not supporting set operations.
 *   <li><b>Quantiles sketch</b> ({@link UpdateDoublesSketch}) — response time percentiles (p50,
 *       p95, p99) in O(log N) space regardless of the number of events.
 *   <li><b>Frequency sketch</b> ({@link ItemsSketch}) — top-K most visited pages using a
 *       space-saving algorithm that guarantees no false negatives above a threshold.
 * </ul>
 *
 * <p>All methods are {@code synchronized} because sketches are not thread-safe.
 */
@Service
public class SketchService {

  // lgK=12 → HLL uses ~5 KB and achieves ~0.8% standard error
  private static final int HLL_LG_K = 12;
  // maxMapSize for frequency sketch — trades memory for accuracy
  private static final int FREQUENCY_MAP_SIZE = 128;
  // Number of std deviations for confidence bounds (2 → ~95%)
  private static final int CONFIDENCE_STD_DEVS = 2;

  private UpdateSketch visitorSketch;
  private HllSketch pageSketch;
  private UpdateDoublesSketch quantilesSketch;
  private ItemsSketch<String> pageFrequencySketch;

  public SketchService() {
    reset();
  }

  /** Records an event, updating whichever sketches have relevant data. */
  public synchronized void record(Event event) {
    if (StringUtils.hasText(event.userId())) {
      visitorSketch.update(event.userId());
    }
    if (StringUtils.hasText(event.pageUrl())) {
      pageSketch.update(event.pageUrl());
      pageFrequencySketch.update(event.pageUrl());
    }
    if (event.responseTimeMs() != null) {
      quantilesSketch.update(event.responseTimeMs());
    }
  }

  /**
   * Returns a unique visitor estimate from the Theta sketch, with 95% confidence bounds.
   *
   * <p>Theta sketches retain a random sample of hashed values. As more items are added the
   * retention threshold (theta) shrinks, keeping the sample at a fixed size. The estimate is
   * derived from the sample size divided by theta.
   */
  public synchronized UniqueEstimate uniqueVisitors() {
    return new UniqueEstimate(
        (long) visitorSketch.getEstimate(),
        (long) visitorSketch.getLowerBound(CONFIDENCE_STD_DEVS),
        (long) visitorSketch.getUpperBound(CONFIDENCE_STD_DEVS));
  }

  /**
   * Returns a unique page estimate from the HLL sketch, with 95% confidence bounds.
   *
   * <p>HyperLogLog uses a fixed-size register array (2^lgK entries) to track leading-zero patterns
   * in hashed values. At lgK=12 the sketch uses ~5 KB regardless of cardinality.
   */
  public synchronized UniqueEstimate uniquePages() {
    return new UniqueEstimate(
        (long) pageSketch.getEstimate(),
        (long) pageSketch.getLowerBound(CONFIDENCE_STD_DEVS),
        (long) pageSketch.getUpperBound(CONFIDENCE_STD_DEVS));
  }

  /**
   * Returns response time percentiles from the Quantiles sketch.
   *
   * <p>The sketch maintains a compact summary structure that answers rank/quantile queries in O(log
   * N) space and O(log log N) time, with a rank error bounded by 1/(2*k).
   */
  public synchronized ResponseTimeStats responseTimes() {
    if (quantilesSketch.isEmpty()) {
      return new ResponseTimeStats(0, 0, 0, 0);
    }
    return new ResponseTimeStats(
        quantilesSketch.getQuantile(0.50),
        quantilesSketch.getQuantile(0.95),
        quantilesSketch.getQuantile(0.99),
        quantilesSketch.getN());
  }

  /**
   * Returns the top {@code limit} most visited pages from the Frequency sketch.
   *
   * <p>{@link ErrorType#NO_FALSE_NEGATIVES} ensures every page that appears more than the error
   * threshold times is included in the result; some lower-frequency pages may also appear.
   */
  public synchronized List<PageFrequency> popularPages(int limit) {
    return Arrays.stream(pageFrequencySketch.getFrequentItems(ErrorType.NO_FALSE_NEGATIVES))
        .limit(limit)
        .map(row -> new PageFrequency(row.getItem(), row.getEstimate()))
        .toList();
  }

  /** Returns a full analytics snapshot across all four sketches. */
  public synchronized AnalyticsSummary summary(int popularPagesLimit) {
    return new AnalyticsSummary(
        uniqueVisitors(), uniquePages(), responseTimes(), popularPages(popularPagesLimit));
  }

  /** Resets all sketches, discarding all recorded events. */
  public synchronized void reset() {
    visitorSketch = UpdateSketch.builder().build();
    pageSketch = new HllSketch(HLL_LG_K);
    quantilesSketch = DoublesSketch.builder().build();
    pageFrequencySketch = new ItemsSketch<>(FREQUENCY_MAP_SIZE);
  }
}
