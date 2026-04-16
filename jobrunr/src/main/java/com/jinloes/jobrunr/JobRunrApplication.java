package com.jinloes.jobrunr;

import com.jinloes.jobrunr.service.ReportService;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JobRunrApplication {

  public static void main(String[] args) {
    SpringApplication.run(JobRunrApplication.class, args);
  }

  // Register a recurring cleanup job on startup — runs every minute for demo purposes.
  // JobRunr resolves ReportService from the Spring context at execution time using the
  // IocJobLambda pattern, so the lambda is serializable even though ReportService is a bean.
  @Bean
  CommandLineRunner scheduleRecurringJobs(JobScheduler jobScheduler) {
    return args ->
        jobScheduler.<ReportService>scheduleRecurrently(
            "cleanup-job", "* * * * *", ReportService::cleanupOldReports);
  }
}
