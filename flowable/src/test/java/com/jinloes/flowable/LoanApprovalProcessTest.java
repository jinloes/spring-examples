package com.jinloes.flowable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LoanApprovalProcessTest {

  @Autowired RuntimeService runtimeService;
  @Autowired TaskService taskService;
  @Autowired HistoryService historyService;

  /** High income (>= $100k) → credit score 750 → auto-approve path, no user task. */
  @Test
  void highCreditScore_autoApproves() {
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey(
            "loanApproval",
            Map.of("applicantName", "Alice High", "loanAmount", 80_000, "annualIncome", 150_000));

    HistoricProcessInstance historic = finishedInstance(instance.getId());
    assertThat(historic).isNotNull();
    assertThat(decisionVariable(instance.getId())).isEqualTo("APPROVED");
  }

  /** Low income (< $30k) → credit score 400 → auto-reject path, no user task. */
  @Test
  void lowCreditScore_autoRejects() {
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey(
            "loanApproval",
            Map.of("applicantName", "Bob Low", "loanAmount", 10_000, "annualIncome", 20_000));

    HistoricProcessInstance historic = finishedInstance(instance.getId());
    assertThat(historic).isNotNull();
    assertThat(decisionVariable(instance.getId())).isEqualTo("REJECTED");
  }

  /** Mid income ($60-99k) → credit score 650 → pauses at Manager Review user task. */
  @Test
  void mediumCreditScore_createsManagerReviewTask() {
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey(
            "loanApproval",
            Map.of("applicantName", "Carol Mid", "loanAmount", 40_000, "annualIncome", 75_000));

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(instance.getId()).list();

    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Manager Review");

    // Clean up: complete the task so the context stays tidy for other tests
    taskService.complete(tasks.get(0).getId(), Map.of("approved", false));
  }

  /** Manager approves a borderline application → process completes as APPROVED. */
  @Test
  void managerApproves_completesAsApproved() {
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey(
            "loanApproval",
            Map.of("applicantName", "Dave Review", "loanAmount", 30_000, "annualIncome", 65_000));

    Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId(), Map.of("approved", true));

    HistoricProcessInstance historic = finishedInstance(instance.getId());
    assertThat(historic).isNotNull();
    assertThat(decisionVariable(instance.getId())).isEqualTo("APPROVED");
  }

  /** Manager rejects a borderline application → process completes as REJECTED. */
  @Test
  void managerRejects_completesAsRejected() {
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey(
            "loanApproval",
            Map.of("applicantName", "Eve Review", "loanAmount", 50_000, "annualIncome", 62_000));

    Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
    assertThat(task).isNotNull();

    taskService.complete(task.getId(), Map.of("approved", false));

    HistoricProcessInstance historic = finishedInstance(instance.getId());
    assertThat(historic).isNotNull();
    assertThat(decisionVariable(instance.getId())).isEqualTo("REJECTED");
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private HistoricProcessInstance finishedInstance(String id) {
    return historyService
        .createHistoricProcessInstanceQuery()
        .processInstanceId(id)
        .finished()
        .singleResult();
  }

  private Object decisionVariable(String processInstanceId) {
    return historyService
        .createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .variableName("decision")
        .singleResult()
        .getValue();
  }
}
