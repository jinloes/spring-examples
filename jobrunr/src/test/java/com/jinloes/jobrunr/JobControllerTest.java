package com.jinloes.jobrunr;

import static org.assertj.core.api.Assertions.assertThat;

import com.jinloes.jobrunr.model.EmailRequest;
import com.jinloes.jobrunr.model.JobResponse;
import com.jinloes.jobrunr.model.ReportRequest;
import com.jinloes.jobrunr.model.ScheduleReportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JobControllerTest {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void enqueueReportJobReturnsJobId() {
    var response =
        restTemplate.postForObject("/jobs/report", new ReportRequest("MONTHLY"), JobResponse.class);
    assertThat(response).isNotNull();
    assertThat(response.jobId()).isNotBlank();
    assertThat(response.message()).containsIgnoringCase("enqueued");
  }

  @Test
  void scheduleDelayedReportJobReturnsJobId() {
    var response =
        restTemplate.postForObject(
            "/jobs/report/scheduled", new ScheduleReportRequest("WEEKLY", 60), JobResponse.class);
    assertThat(response).isNotNull();
    assertThat(response.jobId()).isNotBlank();
    assertThat(response.message()).containsIgnoringCase("60 seconds");
  }

  @Test
  void enqueueEmailJobReturnsJobId() {
    var response =
        restTemplate.postForObject(
            "/jobs/email",
            new EmailRequest("user@example.com", "Report Ready", "Your report is ready."),
            JobResponse.class);
    assertThat(response).isNotNull();
    assertThat(response.jobId()).isNotBlank();
  }
}
