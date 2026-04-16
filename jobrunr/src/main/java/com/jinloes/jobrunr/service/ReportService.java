package com.jinloes.jobrunr.service;

import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportService {

  // %0 in the job name is replaced by the first argument at runtime, visible in the dashboard.
  @Job(name = "Generate %0 report")
  public void generateReport(String reportType) throws InterruptedException {
    log.info("Starting {} report generation...", reportType);
    Thread.sleep(2000); // simulate work
    log.info("{} report generated successfully", reportType);
  }

  @Job(name = "Send email to %0")
  public void sendEmailNotification(String recipient, String subject, String body)
      throws InterruptedException {
    log.info("Sending email to {} — subject: {}", recipient, subject);
    Thread.sleep(500); // simulate SMTP call
    log.info("Email sent to {}", recipient);
  }

  @Job(name = "Cleanup old reports")
  public void cleanupOldReports() {
    log.info("Running scheduled cleanup of reports older than 30 days...");
    log.info("Cleanup complete");
  }
}
