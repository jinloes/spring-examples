# flowable — Architecture

Reference and design details for `flowable`. See [AGENTS.md](AGENTS.md) for build, run, and test instructions.

## What is Flowable?

Flowable is an open-source **Business Process Management (BPM)** and workflow engine for Java. It
executes processes modelled in **BPMN 2.0** (Business Process Model and Notation), an industry
standard XML format for describing workflows as a directed graph of tasks, gateways, and events.

At runtime, Flowable manages process instances: it tracks which step each instance is on, persists
state to a database, and routes execution based on conditions. This makes it possible to model
long-running processes that span seconds, days, or months and survive server restarts.

### Good use cases

| Domain | Example workflows |
|--------|-------------------|
| Finance | Loan approval, expense reimbursement, trade settlement |
| HR | Employee onboarding, leave requests, performance reviews |
| Operations | Order fulfillment, incident management, change requests |
| Compliance | Document approval chains, audit workflows, KYC checks |
| IT | Ticket escalation, deployment approval gates, access provisioning |

Flowable is a good fit when:
- A process involves **human tasks** that pause execution until someone acts
- Decisions branch on **business rules** (credit score, approval threshold, role)
- A workflow must be **auditable** — you need a history of what happened and when
- Steps may be **long-running** or asynchronous (wait for external events, retries)
- The process logic should be **visible to non-developers** via the BPMN diagram

It is less suited to pure in-process orchestration where a simple state machine or chain of service
calls would suffice.

## Process: `loanApproval`

```
Start ──► Credit Score Check ──► [Gateway: score?]
                                    >= 700  ─────────────────────► Auto Approve ──► End
                                    500-699 ──► Manager Review ──► [Gateway: decision?]
                                    │                                  approved ──► Notify Approval ──► End
                                    │                                  rejected ──► Notify Rejection ──► End
                                    < 500   ─────────────────────► Auto Reject ──► End
```

| Score range | Route | Decision |
|-------------|-------|----------|
| >= 700 | Auto-approve | `APPROVED` |
| 500–699 | Manager review (user task) | `APPROVED` or `REJECTED` |
| < 500 | Auto-reject | `REJECTED` |

The credit score is derived from `annualIncome`:

| Annual income | Credit score |
|---------------|--------------|
| >= $100,000 | 750 |
| >= $60,000 | 650 |
| >= $30,000 | 550 |
| < $30,000 | 400 |

## Endpoints

### Submit a loan application
```
POST /loans
Content-Type: application/json

{
  "applicantName": "Jane Doe",
  "loanAmount": 50000,
  "annualIncome": 75000
}
```
Returns `{ "processInstanceId": "...", "message": "..." }`.

### Check process status
```
GET /loans/{processInstanceId}/status
```
Returns `{ "status": "IN_PROGRESS" | "COMPLETED", "variables": {...}, "endTime": "..." }`.

### List pending manager-review tasks
```
GET /loans/tasks
```
Returns tasks waiting for human decision (score in the 500–699 range).

### Complete a manager-review task
```
POST /loans/tasks/{taskId}/complete
Content-Type: application/json

{ "approved": true }    ← approve
{ "approved": false }   ← reject
```

## Key Classes

| Class | Role |
|-------|------|
| `CreditScoreDelegate` | Service task — calculates credit score, sets `creditScore` variable |
| `ApprovalNotificationDelegate` | Service task — logs approval, sets `decision = "APPROVED"` |
| `RejectionNotificationDelegate` | Service task — logs rejection, sets `decision = "REJECTED"` |
| `LoanController` | REST API — submit, status, list tasks, complete task |

Delegates are Spring beans looked up by name via `flowable:delegateExpression="${beanName}"` in the
BPMN XML. The process definition is auto-deployed from
`src/main/resources/processes/loan-approval.bpmn20.xml` on startup.
