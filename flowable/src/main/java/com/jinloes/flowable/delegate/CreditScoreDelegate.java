package com.jinloes.flowable.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Service task: calculates a simplified credit score based on the applicant's annual income and
 * stores it as the {@code creditScore} process variable. Downstream gateways route the process
 * based on this value.
 */
@Slf4j
@Component("creditScoreDelegate")
public class CreditScoreDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    int annualIncome = (int) execution.getVariable("annualIncome");
    int creditScore = calculateCreditScore(annualIncome);

    log.info(
        "Credit score {} calculated for applicant: {}",
        creditScore,
        execution.getVariable("applicantName"));

    execution.setVariable("creditScore", creditScore);
  }

  /** Simple scoring model: buckets annual income into four score tiers. */
  private int calculateCreditScore(int annualIncome) {
    if (annualIncome >= 100_000) return 750;
    if (annualIncome >= 60_000) return 650;
    if (annualIncome >= 30_000) return 550;
    return 400;
  }
}
