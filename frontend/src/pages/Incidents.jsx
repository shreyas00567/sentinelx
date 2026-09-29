import { useState } from 'react'
import { api, useFetch, Badge, RiskPill, Pagination, Drawer, fmtTime } from '../components/ui'
import { useAuth } from '../auth'

const STATUSES = ['', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']

export default function Incidents() {
  const { user } = useAuth()
  const [filters, setFilters] = useState({ status: '', q: '' })
  const [applied, setApplied] = useState(filters)
  const [page, setPage] = useState(0)
  const [selectedId, setSelectedId] = useState(null)
  const [notes, setNotes] = useState('')
  const [assignee, setAssignee] = useState('')
  const [busy, setBusy] = useState(false)

  const { data, error, loading, reload } = useFetch(
    () => api('/api/incidents', { params: { ...applied, page, size: 25 } }),
    [applied, page]
  )

  const { data: detail } = useFetch(
    () => selectedId ? api(`/api/incidents/${selectedId}`) : Promise.resolve(null),
    [selectedId]
  )

  useEffectOnce(detail, v => { setNotes(''); setAssignee(v?.assignedTo || user.username || '') })

  function apply(e) {
    e.preventDefault()
    setPage(0); setApplied(filters)
  }

  async function save(update) {
    if (!detail) return
    setBusy(true)
    try {
      await api(`/api/incidents/${detail.id}`, {
        method: 'PUT',
        body: {
          status: update.status ?? detail.status,
          assignedTo: update.assignedTo ?? assignee,
          notesAppend: update.notesAppend ?? '',
          title: update.title ?? detail.title
        }
      })
      reload()
      setSelectedId(null)
    } catch (ex) { alert(ex.message) }
    finally { setBusy(false) }
  }

  return (
    <>
      <div className="toolbar">
        <form onSubmit={apply} style={{ display: 'flex', gap: 10 }}>
          <input type="text" placeholder="Search title / IP / assignee…" value={filters.q}
            onChange={e => setFilters(f => ({ ...f, q: e.target.value }))} />
          <select value={filters.status} onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}>
            {STATUSES.map(s => <option key={s} value={s}>{s || 'All statuses'}</option>)}
          </select>
          <button className="btn">Apply</button>
        </form>
      </div>

      {error && <div className="error-box">{error}</div>}

      <div className="card">
        {loading ? <div className="loading">Loading…</div> : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Risk</th><th>Title</th><th>Type</th><th>Status</th>
                    <th>Source IP</th><th>Assigned</th><th>Alerts</th><th>Detection</th><th>Updated</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.content || []).map(inc => (
                    <tr key={inc.id} className="clickable" onClick={() => setSelectedId(inc.id)}>
                      <td><RiskPill score={inc.riskScore} /></td>
                      <td>{inc.title}</td>
                      <td>{inc.attackType}</td>
                      <td><Badge value={inc.status} /></td>
                      <td className="mono">{inc.sourceIp}</td>
                      <td>{inc.assignedTo || <span className="dim">unassigned</span>}</td>
                      <td>{inc.alertCount}</td>
                      <td><Badge value={inc.detectionMethod} /></td>
                      <td className="dim">{fmtTime(inc.updatedAt)}</td>
                      <td>›</td>
                    </tr>
                  ))}
                  {!data?.content?.length && (
                    <tr><td colSpan={10} className="empty">No incidents found.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} size={data?.size || 25} total={data?.totalElements || 0} onPage={setPage} />
          </>
        )}
      </div>

      {detail && (
        <Drawer onClose={() => setSelectedId(null)}>
          <h2>{detail.title}</h2>
          <p><Badge value={detail.severity} /> <Badge value={detail.status} /> <Badge value={detail.detectionMethod} /></p>

          <dl className="kv">
            <dt>Risk Score</dt><dd><RiskPill score={detail.riskScore} /></dd>
            <dt>Attack Type</dt><dd>{detail.attackType}</dd>
            <dt>Source IP</dt><dd className="mono">{detail.sourceIp}</dd>
            <dt>Target</dt><dd className="mono">{detail.target || '—'}</dd>
            <dt>AI Score</dt><dd>{detail.aiScore != null ? detail.aiScore : '—'}</dd>
            <dt>Correlated Alerts</dt><dd>{detail.alertCount}</dd>
            <dt>Created</dt><dd>{fmtTime(detail.createdAt)}</dd>
            {detail.resolvedAt && <><dt>Resolved</dt><dd>{fmtTime(detail.resolvedAt)}</dd></>}
          </dl>

          {detail.explanation && (
            <>
              <div className="section-title">What Happened</div>
              <pre className="code" style={{ color: '#e2e8f0' }}>{detail.explanation}</pre>
            </>
          )}
          {detail.recommendedActions?.length > 0 && (
            <>
              <div className="section-title">Recommended Actions</div>
              <ul style={{ paddingLeft: 20, lineHeight: 1.9 }}>
                {detail.recommendedActions.map((a, i) => <li key={i}>{a}</li>)}
              </ul>
            </>
          )}
          {detail.evidence && (
            <>
              <div className="section-title">Evidence</div>
              <pre className="code">{typeof detail.evidence === 'string'
                ? detail.evidence : JSON.stringify(detail.evidence, null, 2)}</pre>
            </>
          )}
          {detail.notes && (
            <>
              <div className="section-title">Investigation Notes</div>
              <pre className="code" style={{ color: '#e2e8f0', whiteSpace: 'pre-wrap' }}>{detail.notes}</pre>
            </>
          )}

          <div className="section-title">Update Incident</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <select value={detail.status} onChange={e => save({ status: e.target.value })}>
              {STATUSES.filter(Boolean).map(s =>
                <option key={s} value={s} disabled={s === detail.status}>{s}{s === detail.status ? ' (current)' : ''}</option>)}
            </select>
            <input type="text" placeholder="Assign to…" value={assignee}
              onChange={e => setAssignee(e.target.value)} />
            <textarea rows={3} placeholder="Append investigation note…" value={notes}
              onChange={e => setNotes(e.target.value)} />
            <button className="btn" disabled={busy}
              onClick={() => save({ notesAppend: notes, assignedTo: assignee })}>
              Save Changes
            </button>
          </div>
        </Drawer>
      )}
    </>
  )
}

function useEffectOnce(dep, fn) {
  const [done, setDone] = useState(null)
  if (dep !== done && dep !== null && dep !== undefined) {
    setDone(dep)
    fn(dep)
  }
}
