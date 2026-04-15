import http from "k6/http";
import { check, sleep } from "k6";
import { Trend } from "k6/metrics";

const fanoutDuration = new Trend("fanout_duration", true);
const blockingDuration = new Trend("blocking_duration", true);
const heapUsedMb = new Trend("heap_used_mb", true);
const processVirtualMb = new Trend("process_virtual_mb", true);

/**
 * Stress test for the loom-web virtual threads demo.
 *
 * Scenarios:
 *   blocking      - 1000 concurrent users each hitting a 1-second blocking endpoint.
 *                   Both profiles allow 1000 concurrent threads, but platform threads
 *                   create 1000 OS threads (~512KB native stack each = ~500MB native RSS)
 *                   while virtual threads use only a handful of carrier threads — so
 *                   RSS should be dramatically lower with virtual threads.
 *
 *   fanout        - 50 concurrent users each triggering 10 parallel downstream calls
 *                   (500ms each). Should complete in ~500ms regardless of fan-out width.
 *
 *   memoryPoller  - 1 VU sampling JVM heap from Spring Actuator every second.
 *                   Reports peak and average heap in the final summary.
 *
 * Usage:
 *   Start the app:  ./gradlew :loom-web:bootRun
 *   Run tests:      ./gradlew :loom-web:k6
 *
 * To compare platform vs virtual threads:
 *   1. Run with virtual threads (default):          ./gradlew :loom-web:k6
 *   2. Set spring.threads.virtual.enabled=false in application.yaml, restart
 *   3. Run again:                                   ./gradlew :loom-web:k6
 *   4. Compare the summary — p95 blocking latency and peak heap both change.
 */
export const options = {
  scenarios: {
    blocking: {
      executor: "constant-vus",
      vus: 1000,
      duration: "60s",
      exec: "blocking",
    },
    fanout: {
      executor: "constant-vus",
      vus: 50,
      duration: "60s",
      exec: "fanout",
      startTime: "65s",
    },
    memoryPoller: {
      executor: "constant-vus",
      vus: 1,
      duration: "125s",
      exec: "memoryPoller",
    },
  },
  thresholds: {
    blocking_duration: ["p(95)<1500"], // passes with VT, fails with PT (500-thread cap)
    fanout_duration: ["p(95)<1000"],
  },
};

export function blocking() {
  const res = http.get("http://localhost:8080/threads/blocking?delayMs=1000");
  check(res, { "status 200": (r) => r.status === 200 });
  blockingDuration.add(res.timings.duration);
}

export function fanout() {
  const res = http.get(
    "http://localhost:8080/threads/fanout?tasks=10&delayMs=500"
  );
  check(res, { "status 200": (r) => r.status === 200 });
  fanoutDuration.add(res.timings.duration);
}

export function memoryPoller() {
  const res = http.get("http://localhost:8080/threads/memory");
  if (res.status === 200) {
    const body = JSON.parse(res.body);
    heapUsedMb.add(body.heapUsedMb);
    processVirtualMb.add(body.processRssMb);
  }
  sleep(1);
}

export function handleSummary(data) {
  const threadInfo = http.get("http://localhost:8080/threads/info");
  const virtualThreads =
    threadInfo.status === 200
      ? JSON.parse(threadInfo.body).virtual
      : "unknown";

  const blocking = data.metrics["blocking_duration"];
  const fanout = data.metrics["fanout_duration"];
  const heap = data.metrics["heap_used_mb"];
  const procVirtual = data.metrics["process_virtual_mb"];

  const fmt = (ms) => (ms !== undefined ? `${Math.round(ms)}ms` : "n/a");
  const fmtMb = (mb) => (mb !== undefined ? `${Math.round(mb)}MB` : "n/a");

  const blockingP95 = blocking?.values?.["p(95)"];
  const blockingThreshold = 1500;
  const blockingPass =
    blockingP95 !== undefined && blockingP95 < blockingThreshold;

  const fanoutP95 = fanout?.values?.["p(95)"];
  const fanoutThreshold = 1000;
  const fanoutPass = fanoutP95 !== undefined && fanoutP95 < fanoutThreshold;

  const bP50 = fmt(blocking?.values?.["p(50)"]);
  const bP95 = fmt(blockingP95);
  const bP99 = fmt(blocking?.values?.["p(99)"]);
  const fP50 = fmt(fanout?.values?.["p(50)"]);
  const fP95 = fmt(fanoutP95);
  const fP99 = fmt(fanout?.values?.["p(99)"]);
  const bResult = blockingPass ? "✓ PASS" : "✗ FAIL — thread pool saturated";
  const fResult = fanoutPass ? "✓ PASS" : "✗ FAIL";

  const heapPeak = fmtMb(heap?.values?.max);
  const heapAvg = fmtMb(heap?.values?.avg);
  const heapMin = fmtMb(heap?.values?.min);
  const procPeak = fmtMb(procVirtual?.values?.max);
  const procAvg = fmtMb(procVirtual?.values?.avg);
  const procMin = fmtMb(procVirtual?.values?.min);

  const summary = `
╔══════════════════════════════════════════════════════════╗
║              loom-web Virtual Threads Summary            ║
╠══════════════════════════════════════════════════════════╣
║  Virtual threads enabled: ${String(virtualThreads).padEnd(30)}║
╠══════════════════════════════════════════════════════════╣
║  BLOCKING scenario (1000 VUs, 1000 threads, 60s)         ║
║    p50 : ${bP50.padEnd(48)}║
║    p95 : ${bP95.padEnd(48)}║
║    p99 : ${bP99.padEnd(48)}║
║    threshold (<${blockingThreshold}ms) : ${bResult.padEnd(35)}║
╠══════════════════════════════════════════════════════════╣
║  FANOUT scenario (50 VUs x 10 tasks x 500ms delay)      ║
║    p50 : ${fP50.padEnd(48)}║
║    p95 : ${fP95.padEnd(48)}║
║    p99 : ${fP99.padEnd(48)}║
║    threshold (<${fanoutThreshold}ms) : ${fResult.padEnd(37)}║
╠══════════════════════════════════════════════════════════╣
║  JVM HEAP (sampled every 1s — virtual thread stacks)    ║
║    min  : ${heapMin.padEnd(48)}║
║    avg  : ${heapAvg.padEnd(48)}║
║    peak : ${heapPeak.padEnd(48)}║
╠══════════════════════════════════════════════════════════╣
║  PROCESS RSS — actual physical memory in use            ║
║    min  : ${procMin.padEnd(48)}║
║    avg  : ${procAvg.padEnd(48)}║
║    peak : ${procPeak.padEnd(48)}║
╠══════════════════════════════════════════════════════════╣
║  TIP: compare k6Virtual vs k6Platform (both 1000 threads)║
║  Expect: RSS lower with VT (few carrier threads vs 1000 ║
║          OS threads @ ~512KB each = ~500MB native RSS)  ║
╚══════════════════════════════════════════════════════════╝
`;

  return { stdout: summary };
}