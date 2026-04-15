package com.jinloes.loom_web;

import com.jinloes.loom_web.model.FanoutResponse;
import com.jinloes.loom_web.model.MemoryInfo;
import com.jinloes.loom_web.model.ServiceResult;
import com.jinloes.loom_web.model.ThreadInfo;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates Project Loom virtual threads in a Spring MVC application.
 *
 * <p>Virtual threads are enabled via {@code spring.threads.virtual.enabled=true} in
 * application.yaml. Spring Boot automatically configures Tomcat to dispatch each request on a
 * virtual thread instead of a platform thread from a fixed-size pool.
 *
 * <p>Key insight: virtual threads are cheap. The JVM can create millions of them. When a virtual
 * thread blocks on I/O (a database call, HTTP request, sleep, etc.), it is unmounted from its
 * carrier platform thread, freeing it to run other virtual threads. This gives you the throughput
 * of reactive/async code with the simplicity of blocking, imperative code.
 */
@Slf4j
@RestController
@RequestMapping("/threads")
public class ThreadController {

  /**
   * Returns information about the thread handling this request. With virtual threads enabled the
   * {@code virtual} field will be {@code true} and the thread name will follow the pattern {@code
   * tomcat-handler-N}.
   */
  @GetMapping("/info")
  public ThreadInfo info() {
    Thread t = Thread.currentThread();
    log.info("Request handled by thread: {} (virtual={})", t.getName(), t.isVirtual());
    return new ThreadInfo(t.getName(), t.isVirtual(), t.threadId());
  }

  /**
   * Simulates calling {@code tasks} downstream services concurrently, each taking {@code delayMs}
   * milliseconds. All tasks run in parallel on virtual threads, so total elapsed time ≈ one task's
   * delay regardless of how many tasks there are.
   *
   * <p>Example: 10 tasks × 500 ms each completes in ~500 ms, not ~5000 ms.
   */
  @GetMapping("/fanout")
  public FanoutResponse fanout(
      @RequestParam(defaultValue = "5") int tasks, @RequestParam(defaultValue = "500") long delayMs)
      throws Exception {

    long start = System.currentTimeMillis();
    List<Future<ServiceResult>> futures = new ArrayList<>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 1; i <= tasks; i++) {
        int taskId = i;
        futures.add(executor.submit(() -> callService(taskId, delayMs)));
      }

      List<ServiceResult> results = new ArrayList<>();
      for (Future<ServiceResult> f : futures) {
        results.add(f.get());
      }

      long elapsed = System.currentTimeMillis() - start;
      log.info("{} tasks x {}ms each completed in {}ms total", tasks, delayMs, elapsed);
      return new FanoutResponse(tasks, delayMs, elapsed, results);
    }
  }

  /**
   * Simulates a single blocking I/O operation (e.g. a slow database query). Because Tomcat
   * dispatches requests on virtual threads, thousands of concurrent requests can be in-flight
   * simultaneously without exhausting the platform thread pool.
   */
  @GetMapping("/blocking")
  public Map<String, Object> blocking(@RequestParam(defaultValue = "1000") long delayMs)
      throws InterruptedException {
    Thread t = Thread.currentThread();
    log.info("Blocking for {}ms on thread: {} (virtual={})", delayMs, t.getName(), t.isVirtual());
    Thread.sleep(delayMs);
    return Map.of("thread", t.getName(), "virtual", t.isVirtual(), "delayMs", delayMs);
  }

  /**
   * Returns JVM heap usage and process RSS (Resident Set Size — actual physical pages in use). RSS
   * is the most accurate measure of real memory cost and captures native thread stacks that heap
   * metrics miss.
   *
   * <p>With platform threads: RSS is higher because each Tomcat thread carries a ~1MB OS stack.
   * With virtual threads: RSS is lower; fewer carrier threads means fewer native stacks.
   */
  @GetMapping("/memory")
  public MemoryInfo memory() throws Exception {
    Runtime rt = Runtime.getRuntime();
    long heapUsed = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
    long heapMax = rt.maxMemory() / 1024 / 1024;
    long jvmCommitted =
        ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted()
            + ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getCommitted();
    jvmCommitted /= 1024 * 1024;

    long pid = ProcessHandle.current().pid();
    Process ps = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(pid)).start();
    long rssKb = Long.parseLong(new String(ps.getInputStream().readAllBytes()).trim());
    long rssMb = rssKb / 1024;

    return new MemoryInfo(heapUsed, heapMax, jvmCommitted, rssMb);
  }

  private ServiceResult callService(int taskId, long delayMs) throws InterruptedException {
    Thread t = Thread.currentThread();
    Thread.sleep(delayMs); // simulates a blocking external call
    return new ServiceResult(taskId, "service-" + taskId, t.getName(), t.isVirtual());
  }
}
