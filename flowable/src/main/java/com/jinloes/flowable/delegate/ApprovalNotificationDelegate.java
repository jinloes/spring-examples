package com.jinloes.flowable.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Service task: records the final approval decision and simulates sending a notification. Used for
 * both the auto-approval path (high credit score) and after a manager explicitly approves.
 */
@Slf4j
@Component("approvalNotificationDelegate")
public class ApprovalNotificationDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    String applicantName = (String) execution.getVariable("applicantName");
    int loanAmount = (int) execution.getVariable("loanAmount");
    int creditScore = (int) execution.getVariable("creditScore");

    log.info(
        "APPROVED: Loan of ${} approved for {} (credit score: {})",
        loanAmount,
        applicantName,
        creditScore);

    execution.setVariable("decision", "APPROVED");
  }
}
