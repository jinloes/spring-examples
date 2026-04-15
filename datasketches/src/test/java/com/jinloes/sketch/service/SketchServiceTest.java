package com.jinloes.sketch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.jinloes.sketch.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SketchServiceTest {

  private SketchService service;

  @BeforeEach
  void setUp() {
    service = new SketchService();
  }

  // ---------------------------------------------------------------------------
  // Unique visitors (Theta sketch)
  // ---------------------------------------------------------------------------

  @Nested
  class UniqueVisitors {

    @Test
    void returnsZeroWithNoEvents() {
      assertThat(service.uniqueVisitors().estimate()).isZero();
    }

    @Test
    void countsDistinctUserIds() {
      service.record(new Event("user-a", null, null));
      service.record(new Event("user-b", null, null));
      service.record(new Event("user-a", null, null)); // duplicate

      assertThat(service.uniqueVisitors().estimate()).isEqualTo(2);
    }

    @Test
    void estimateIsAccurateForLargeN() {
      // Insert 10,000 distinct users — Theta sketch should be within 2% of the true count.
      for (int i = 0; i < 10_000; i++) {
        service.record(new Event("user-" + i, null, null));
      }
      assertThat(service.uniqueVisitors().estimate()).isCloseTo(10_000L, within(200L));
    }

    @Test
    void confidenceBoundsStraddleEstimate() {
      for (int i = 0; i < 1_000; i++) {
        service.record(new Event("user-" + i, null, null));
      }
      var result = service.uniqueVisitors();
      assertThat(result.lowerBound()).isLessThanOrEqualTo(result.estimate());
      assertThat(result.upperBound()).isGreaterThanOrEqualTo(result.estimate());
    }
  }

  // ---------------------------------------------------------------------------
  // Unique pages (HLL sketch)
  // ---------------------------------------------------------------------------

  @Nested
  class UniquePages {

    @Test
    void returnsZeroWithNoEvents() {
      assertThat(service.uniquePages().estimate()).isZero();
    }

    @Test
    void countsDistinctPageUrls() {
      service.record(new Event(null, "/home", null));
      service.record(new Event(null, "/about", null));
      service.record(new Event(null, "/home", null)); // duplicate

      assertThat(service.uniquePages().estimate()).isEqualTo(2);
    }

    @Test
    void estimateIsAccurateForLargeN() {
      for (int i = 0; i < 10_000; i++) {
        service.record(new Event(null, "/page-" + i, null));
      }
      // HLL at lgK=12 has ~0.8% RSE; allow 5% to stay well inside the confidence bounds
      assertThat(service.uniquePages().estimate()).isCloseTo(10_000L, within(500L));
    }
  }

  // ---------------------------------------------------------------------------
  // Response times (Quantiles sketch)
  // ---------------------------------------------------------------------------

  @Nested
  class ResponseTimes {

    @Test
    void returnsZerosWithNoEvents() {
      var stats = service.responseTimes();
      assertThat(stats.count()).isZero();
      assertThat(stats.p50()).isZero();
    }

    @Test
    void computesPercentilesForUniformDistribution() {
      // Insert 1..1000 ms so p50=~500, p95=~950, p99=~990
      for (int i = 1; i <= 1_000; i++) {
        service.record(new Event(null, null, (double) i));
      }
      var stats = service.responseTimes();
      assertThat(stats.count()).isEqualTo(1_000);
      assertThat(stats.p50()).isCloseTo(500.0, within(20.0));
      assertThat(stats.p95()).isCloseTo(950.0, within(20.0));
      assertThat(stats.p99()).isCloseTo(990.0, within(20.0));
    }

    @Test
    void percentilesAreOrdered() {
      for (int i = 1; i <= 100; i++) {
        service.record(new Event(null, null, (double) i));
      }
      var stats = service.responseTimes();
      assertThat(stats.p50()).isLessThanOrEqualTo(stats.p95());
      assertThat(stats.p95()).isLessThanOrEqualTo(stats.p99());
    }
  }

  // ---------------------------------------------------------------------------
  // Popular pages (Frequency sketch)
  // ---------------------------------------------------------------------------

  @Nested
  class PopularPages {

    @Test
    void returnsEmptyListWithNoEvents() {
      assertThat(service.popularPages(10)).isEmpty();
    }

    @Test
    void returnsTopKPagesByFrequency() {
      // /home visited 100x, /about 50x, /contact 10x
      for (int i = 0; i < 100; i++) service.record(new Event(null, "/home", null));
      for (int i = 0; i < 50; i++) service.record(new Event(null, "/about", null));
      for (int i = 0; i < 10; i++) service.record(new Event(null, "/contact", null));

      var pages = service.popularPages(2);
      assertThat(pages).hasSize(2);
      assertThat(pages.getFirst().page()).isEqualTo("/home");
      assertThat(pages.get(1).page()).isEqualTo("/about");
    }

    @Test
    void limitIsRespected() {
      for (int i = 0; i < 20; i++) {
        service.record(new Event(null, "/page-" + i, null));
      }
      assertThat(service.popularPages(5)).hasSizeLessThanOrEqualTo(5);
    }
  }

  // ---------------------------------------------------------------------------
  // Null / partial events
  // ---------------------------------------------------------------------------

  @Nested
  class NullFields {

    @Test
    void nullUserIdIsIgnoredForVisitorSketch() {
      service.record(new Event(null, "/home", 100.0));
      assertThat(service.uniqueVisitors().estimate()).isZero();
    }

    @Test
    void nullPageUrlIsIgnoredForPageSketches() {
      service.record(new Event("user-1", null, 100.0));
      assertThat(service.uniquePages().estimate()).isZero();
      assertThat(service.popularPages(10)).isEmpty();
    }

    @Test
    void nullResponseTimeIsIgnoredForQuantilesSketch() {
      service.record(new Event("user-1", "/home", null));
      assertThat(service.responseTimes().count()).isZero();
    }
  }

  // ---------------------------------------------------------------------------
  // Reset
  // ---------------------------------------------------------------------------

  @Nested
  class Reset {

    @Test
    void resetClearsAllSketches() {
      service.record(new Event("user-1", "/home", 100.0));
      service.reset();

      assertThat(service.uniqueVisitors().estimate()).isZero();
      assertThat(service.uniquePages().estimate()).isZero();
      assertThat(service.responseTimes().count()).isZero();
      assertThat(service.popularPages(10)).isEmpty();
    }
  }
}
