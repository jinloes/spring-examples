import { useState, useEffect, useCallback } from 'react'

export default function App() {
  const [tab, setTab] = useState('apply')

  return (
    <div className="app">
      <header>
        <h1>Loan Approval Workflow</h1>
        <p>Powered by Flowable BPM + Spring Boot</p>
      </header>

      <nav className="tabs">
        {[
          { id: 'apply', label: 'Apply' },
          { id: 'status', label: 'Check Status' },
          { id: 'tasks', label: 'Manager Tasks' },
        ].map(({ id, label }) => (
          <button
            key={id}
            className={tab === id ? 'active' : ''}
            onClick={() => setTab(id)}
          >
            {label}
          </button>
        ))}
      </nav>

      <main>
        {tab === 'apply' && <ApplyTab />}
        {tab === 'status' && <StatusTab />}
        {tab === 'tasks' && <TasksTab />}
      </main>
    </div>
  )
}

// ── Apply Tab ────────────────────────────────────────────────────────────────

function ApplyTab() {
  const [form, setForm] = useState({ applicantName: '', loanAmount: '', annualIncome: '' })
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setResult(null)
    setError(null)
    try {
      const res = await fetch('/loans', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          applicantName: form.applicantName,
          loanAmount: parseInt(form.loanAmount),
          annualIncome: parseInt(form.annualIncome),
        }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      setResult(await res.json())
      setForm({ applicantName: '', loanAmount: '', annualIncome: '' })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="tab-content">
      <h2>Submit Loan Application</h2>

      <form onSubmit={handleSubmit} className="card form">
        <Field label="Applicant Name">
          <input
            type="text"
            value={form.applicantName}
            onChange={set('applicantName')}
            placeholder="Jane Doe"
            required
          />
        </Field>
        <Field label="Loan Amount ($)">
          <input
            type="number"
            value={form.loanAmount}
            onChange={set('loanAmount')}
            placeholder="50000"
            min="1"
            required
          />
        </Field>
        <Field label="Annual Income ($)">
          <input
            type="number"
            value={form.annualIncome}
            onChange={set('annualIncome')}
            placeholder="75000"
            min="1"
            required
          />
        </Field>
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Submitting…' : 'Submit Application'}
        </button>
      </form>

      {result && (
        <div className="alert alert-success">
          <strong>Application submitted!</strong>
          <p className="muted">Copy this Process ID to check the status later:</p>
          <code>{result.processInstanceId}</code>
        </div>
      )}
      {error && <div className="alert alert-error">Error: {error}</div>}

      <div className="card info-table">
        <h3>Scoring rules</h3>
        <table>
          <thead>
            <tr>
              <th>Annual income</th>
              <th>Credit score</th>
              <th>Outcome</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>≥ $100,000</td>
              <td>750</td>
              <td><span className="badge badge-green">Auto Approve</span></td>
            </tr>
            <tr>
              <td>$60,000 – $99,999</td>
              <td>650</td>
              <td><span className="badge badge-yellow">Manual Review</span></td>
            </tr>
            <tr>
              <td>$30,000 – $59,999</td>
              <td>550</td>
              <td><span className="badge badge-yellow">Manual Review</span></td>
            </tr>
            <tr>
              <td>&lt; $30,000</td>
              <td>400</td>
              <td><span className="badge badge-red">Auto Reject</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Status Tab ───────────────────────────────────────────────────────────────

function StatusTab() {
  const [processId, setProcessId] = useState('')
  const [status, setStatus] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleCheck = async (e) => {
    e.preventDefault()
    setLoading(true)
    setStatus(null)
    setError(null)
    try {
      const res = await fetch(`/loans/${processId.trim()}/status`)
      if (res.status === 404) throw new Error('Process not found')
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      setStatus(await res.json())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="tab-content">
      <h2>Check Application Status</h2>

      <form onSubmit={handleCheck} className="card form">
        <Field label="Process Instance ID">
          <input
            type="text"
            value={processId}
            onChange={(e) => setProcessId(e.target.value)}
            placeholder="Paste the process ID here"
            required
          />
        </Field>
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Checking…' : 'Check Status'}
        </button>
      </form>

      {error && <div className="alert alert-error">Error: {error}</div>}

      {status && (
        <div className="card">
          <StatusBadge status={status.status} />
          {status.variables && Object.keys(status.variables).length > 0 && (
            <table className="vars-table">
              <thead>
                <tr>
                  <th>Variable</th>
                  <th>Value</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(status.variables).map(([k, v]) => (
                  <tr key={k}>
                    <td>{k}</td>
                    <td>
                      {k === 'decision' ? (
                        <span className={`badge ${v === 'APPROVED' ? 'badge-green' : 'badge-red'}`}>{v}</span>
                      ) : k === 'creditScore' ? (
                        <span className={scoreClass(v)}>{v} — {scoreLabel(v)}</span>
                      ) : (
                        String(v)
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {status.endTime && (
            <p className="muted" style={{ marginTop: 12 }}>
              Completed: {new Date(status.endTime).toLocaleString()}
            </p>
          )}
        </div>
      )}
    </div>
  )
}

// ── Tasks Tab ────────────────────────────────────────────────────────────────

function TasksTab() {
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(false)
  const [completing, setCompleting] = useState(null)
  const [error, setError] = useState(null)

  const loadTasks = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch('/loans/tasks')
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      setTasks(await res.json())
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTasks()
  }, [loadTasks])

  const completeTask = async (taskId, approved) => {
    setCompleting(taskId)
    try {
      const res = await fetch(`/loans/tasks/${taskId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approved }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      await loadTasks()
    } catch (err) {
      setError(err.message)
    } finally {
      setCompleting(null)
    }
  }

  return (
    <div className="tab-content">
      <div className="tasks-header">
        <h2>Pending Manager Reviews</h2>
        <button className="btn-secondary" onClick={loadTasks} disabled={loading}>
          {loading ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      {error && <div className="alert alert-error">Error: {error}</div>}

      {!loading && tasks.length === 0 && (
        <div className="empty-state">No applications are waiting for review.</div>
      )}

      {tasks.map((task) => {
        const v = task.variables || {}
        const busy = completing === task.taskId
        return (
          <div key={task.taskId} className="card task-card">
            <div className="task-info">
              <h3>{v.applicantName ?? 'Unknown applicant'}</h3>
              <div className="task-meta">
                <span>
                  Loan: <strong>${(v.loanAmount ?? 0).toLocaleString()}</strong>
                </span>
                <span>
                  Income: <strong>${(v.annualIncome ?? 0).toLocaleString()}</strong>
                </span>
                <span>
                  Credit score:{' '}
                  <strong className={scoreClass(v.creditScore)}>{v.creditScore}</strong>
                </span>
              </div>
              <p className="task-id muted">Task: {task.taskId}</p>
            </div>
            <div className="task-actions">
              <button
                className="btn-approve"
                onClick={() => completeTask(task.taskId, true)}
                disabled={busy}
              >
                Approve
              </button>
              <button
                className="btn-reject"
                onClick={() => completeTask(task.taskId, false)}
                disabled={busy}
              >
                Reject
              </button>
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ── Shared helpers ───────────────────────────────────────────────────────────

function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  )
}

function StatusBadge({ status }) {
  const cls = status === 'IN_PROGRESS' ? 'badge-yellow' : 'badge-green'
  return <span className={`badge ${cls} status-badge`}>{status.replace('_', ' ')}</span>
}

function scoreLabel(score) {
  if (score >= 700) return 'auto-approve'
  if (score >= 500) return 'manual review'
  return 'auto-reject'
}

function scoreClass(score) {
  if (score >= 700) return 'text-green'
  if (score >= 500) return 'text-yellow'
  return 'text-red'
}
