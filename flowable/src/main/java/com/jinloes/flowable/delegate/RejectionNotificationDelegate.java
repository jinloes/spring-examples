package com.jinloes.flowable.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Service task: records the final rejection decision and simulates sending a notification. Used for
 * both the auto-rejection path (low credit score) and after a manager explicitly rejects.
 */
@Slf4j
@Component("rejectionNotificationDelegate")
public class RejectionNotificationDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    String applicantName = (String) execution.getVariable("applicantName");
    int creditScore = (int) execution.getVariable("creditScore");

    log.info("REJECTED: Loan rejected for {} (credit score: {})", applicantName, creditScore);

    execution.setVariable("decision", "REJECTED");
  }
}
