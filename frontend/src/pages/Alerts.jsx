import { useState } from 'react'
import { api, useFetch, Badge, RiskPill, Pagination, Drawer, fmtTime } from '../components/ui'

const SEVERITIES = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']
const STATUSES = ['', 'OPEN', 'ACKNOWLEDGED', 'FALSE_POSITIVE']

export default function Alerts() {
  const [filters, setFilters] = useState({ severity: '', status: '', q: '' })
  const [applied, setApplied] = useState(filters)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState(null)
  const [busy, setBusy] = useState(false)

  const { data, error, loading, reload } = useFetch(
    () => api('/api/alerts', { params: { ...applied, page, size: 25 } }),
    [applied, page]
  )

  function apply(e) {
    e.preventDefault()
    setPage(0)
    setApplied(filters)
  }

  async function updateStatus(alertId, status) {
    setBusy(true)
    try {
      await api(`/api/alerts/${alertId}/status`, { method: 'PUT', body: { status } })
      setSelected(s => s && s.id === alertId ? { ...s, status } : s)
      reload()
    } catch (ex) { alert(ex.message) }
    finally { setBusy(false) }
  }

  return (
    <>
      <div className="toolbar">
        <form onSubmit={apply} style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <input type="text" placeholder="Search IP / type / target…" value={filters.q}
            onChange={e => setFilters(f => ({ ...f, q: e.target.value }))} />
          <select value={filters.severity} onChange={e => setFilters(f => ({ ...f, severity: e.target.value }))}>
            {SEVERITIES.map(s => <option key={s} value={s}>{s || 'All severities'}</option>)}
          </select>
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
                    <th>Risk</th><th>Type</th><th>Detection</th><th>Severity</th><th>Status</th>
                    <th>Source IP</th><th>Target</th><th>Anomaly</th><th>Time</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.content || []).map(a => (
                    <tr key={a.id} className="clickable" onClick={() => setSelected(a)}>
                      <td><RiskPill score={a.riskScore} /></td>
                      <td>{a.attackType}</td>
                      <td><Badge value={a.detectionMethod} /></td>
                      <td><Badge value={a.severity} /></td>
                      <td><Badge value={a.status} /></td>
                      <td className="mono">{a.sourceIp}</td>
                      <td className="mono dim">{a.target}</td>
                      <td>{a.anomalyScore != null ? a.anomalyScore.toFixed(2) : '—'}</td>
                      <td className="dim">{fmtTime(a.createdAt)}</td>
                      <td>›</td>
                    </tr>
                  ))}
                  {!data?.content?.length && (
                    <tr><td colSpan={10} className="empty">No alerts match the current filters.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} size={data?.size || 25} total={data?.totalElements || 0} onPage={setPage} />
          </>
        )}
      </div>

      {selected && (
        <Drawer onClose={() => setSelected(null)}>
          <h2>{selected.attackType} Alert</h2>
          <p><Badge value={selected.severity} /> <Badge value={selected.status} /> <Badge value={selected.detectionMethod} /></p>
          <dl className="kv">
            <dt>Risk Score</dt><dd><RiskPill score={selected.riskScore} /></dd>
            <dt>Confidence</dt><dd>{(selected.confidence * 100).toFixed(0)}%</dd>
            <dt>Source IP</dt><dd className="mono">{selected.sourceIp}</dd>
            <dt>Target</dt><dd className="mono">{selected.target || '—'}</dd>
            <dt>Anomaly Score</dt><dd>{selected.anomalyScore != null ? selected.anomalyScore : '—'}</dd>
            <dt>Detected</dt><dd>{fmtTime(selected.createdAt)}</dd>
          </dl>

          {selected.explanation && (
            <>
              <div className="section-title">Analyst Explanation</div>
              <pre className="code" style={{ color: '#e2e8f0' }}>{selected.explanation}</pre>
            </>
          )}
          {selected.evidence && (
            <>
              <div className="section-title">Evidence</div>
              <pre className="code">{typeof selected.evidence === 'string'
                ? selected.evidence : JSON.stringify(selected.evidence, null, 2)}</pre>
            </>
          )}

          <div className="section-title">Triage Actions</div>
          <div style={{ display: 'flex', gap: 8 }}>
            {['ACKNOWLEDGED', 'FALSE_POSITIVE'].map(st =>
              st !== selected.status &&
              <button key={st} className={`btn sm ${st === 'FALSE_POSITIVE' ? 'secondary' : ''}`}
                disabled={busy} onClick={() => updateStatus(selected.id, st)}>
                Mark {st === 'ACKNOWLEDGED' ? 'Acknowledged' : 'False Positive'}
              </button>
            )}
          </div>
        </Drawer>
      )}
    </>
  )
}
