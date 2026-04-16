package com.jinloes.flowable.controller;

import com.jinloes.flowable.model.LoanApplication;
import com.jinloes.flowable.model.ProcessStatus;
import com.jinloes.flowable.model.TaskSummary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/loans")
public class LoanController {

  private final RuntimeService runtimeService;
  private final TaskService taskService;
  private final HistoryService historyService;

  /** Submit a new loan application — starts the loanApproval process. */
  @PostMapping
  public ResponseEntity<Map<String, String>> submitApplication(
      @RequestBody LoanApplication application) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("applicantName", application.applicantName());
    variables.put("loanAmount", application.loanAmount());
    variables.put("annualIncome", application.annualIncome());

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("loanApproval", variables);

    return ResponseEntity.ok(
        Map.of("processInstanceId", instance.getId(), "message", "Loan application submitted"));
  }

  /**
   * Get the current status and process variables for a loan application. Returns {@code
   * IN_PROGRESS} while the process is running or {@code COMPLETED} once it has finished.
   */
  @GetMapping("/{processInstanceId}/status")
  public ResponseEntity<ProcessStatus> getStatus(@PathVariable String processInstanceId) {
    ProcessInstance instance =
        runtimeService
            .createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();

    if (instance != null) {
      Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
      return ResponseEntity.ok(
          new ProcessStatus(processInstanceId, "IN_PROGRESS", variables, null));
    }

    // Process has ended — look it up in history
    HistoricProcessInstance historic =
        historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();

    if (historic == null) {
      return ResponseEntity.notFound().build();
    }

    Map<String, Object> historicVariables = new HashMap<>();
    historyService
        .createHistoricVariableInstanceQuery()
        .processInstanceId(processInstanceId)
        .list()
        .forEach(v -> historicVariables.put(v.getVariableName(), v.getValue()));

    return ResponseEntity.ok(
        new ProcessStatus(
            processInstanceId, "COMPLETED", historicVariables, historic.getEndTime()));
  }

  /** List all pending manager-review tasks across all active loan applications. */
  @GetMapping("/tasks")
  public ResponseEntity<List<TaskSummary>> getPendingTasks() {
    List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("loanApproval").list();

    List<TaskSummary> summaries =
        tasks.stream()
            .map(
                t ->
                    new TaskSummary(
                        t.getId(),
                        t.getName(),
                        t.getProcessInstanceId(),
                        taskService.getVariables(t.getId())))
            .toList();

    return ResponseEntity.ok(summaries);
  }

  /**
   * Complete a manager-review task. Pass {@code {"approved": true}} to approve or {@code
   * {"approved": false}} to reject the application.
   */
  @PostMapping("/tasks/{taskId}/complete")
  public ResponseEntity<Map<String, String>> completeTask(
      @PathVariable String taskId, @RequestBody Map<String, Object> variables) {

    Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (task == null) {
      return ResponseEntity.notFound().build();
    }

    taskService.complete(taskId, variables);
    return ResponseEntity.ok(Map.of("message", "Task completed"));
  }
}
