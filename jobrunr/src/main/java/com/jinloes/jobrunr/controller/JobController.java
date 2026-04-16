package com.jinloes.jobrunr.controller;

import com.jinloes.jobrunr.model.EmailRequest;
import com.jinloes.jobrunr.model.JobResponse;
import com.jinloes.jobrunr.model.ReportRequest;
import com.jinloes.jobrunr.model.ScheduleReportRequest;
import com.jinloes.jobrunr.service.ReportService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobController {

  private final JobScheduler jobScheduler;
  private final ReportService reportService;

  @PostMapping("/report")
  public ResponseEntity<JobResponse> enqueueReport(@RequestBody ReportRequest request) {
    JobId jobId = jobScheduler.enqueue(() -> reportService.generateReport(request.reportType()));
    log.info("Enqueued report job: {}", jobId);
    return ResponseEntity.ok(
        new JobResponse(jobId.toString(), "Report job enqueued for immediate processing"));
  }

  @PostMapping("/report/scheduled")
  public ResponseEntity<JobResponse> scheduleReport(@RequestBody ScheduleReportRequest request) {
    Instant runAt = Instant.now().plusSeconds(request.delaySeconds());
    JobId jobId =
        jobScheduler.schedule(runAt, () -> reportService.generateReport(request.reportType()));
    log.info("Scheduled report job {} to run at {}", jobId, runAt);
    return ResponseEntity.ok(
        new JobResponse(
            jobId.toString(),
            "Report job scheduled to run in %d seconds".formatted(request.delaySeconds())));
  }

  @PostMapping("/email")
  public ResponseEntity<JobResponse> enqueueEmail(@RequestBody EmailRequest request) {
    JobId jobId =
        jobScheduler.enqueue(
            () ->
                reportService.sendEmailNotification(
                    request.recipient(), request.subject(), request.body()));
    log.info("Enqueued email job: {}", jobId);
    return ResponseEntity.ok(new JobResponse(jobId.toString(), "Email job enqueued"));
  }
}
