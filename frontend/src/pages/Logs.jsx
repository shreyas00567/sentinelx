import { useState } from 'react'
import { api, useFetch, Badge, Pagination, Drawer, fmtTime } from '../components/ui'

const EVENT_TYPES = ['', 'LOGIN_SUCCESS', 'LOGIN_FAILED', 'HTTP_REQUEST', 'HTTP_ERROR',
  'CONNECTION_ATTEMPT', 'FILE_DOWNLOAD']

export default function Logs() {
  const [q, setQ] = useState('')
  const [eventType, setEventType] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState(null)
  const [search, setSearch] = useState('')

  const { data, error, loading } = useFetch(
    () => api('/api/logs', { params: { q: search, eventType, page, size: 50 } }),
    [search, eventType, page]
  )

  function doSearch(e) {
    e.preventDefault()
    setPage(0)
    setSearch(q)
  }

  return (
    <>
      <div className="toolbar">
        <form onSubmit={doSearch} style={{ display: 'flex', gap: 10 }}>
          <input type="text" placeholder="Search IP / endpoint / user…" value={q}
            onChange={e => setQ(e.target.value)} />
          <button className="btn">Search</button>
        </form>
        <select value={eventType} onChange={e => { setEventType(e.target.value); setPage(0) }}>
          {EVENT_TYPES.map(t => <option key={t} value={t}>{t || 'All event types'}</option>)}
        </select>
        <div className="spacer" />
      </div>

      {error && <div className="error-box">{error}</div>}

      <div className="card">
        {loading ? <div className="loading">Loading…</div> : (
          <>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Time</th><th>Type</th><th>Status</th><th>Source</th>
                    <th>User</th><th>Endpoint</th><th>Fails</th><th>Anomaly</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.content || []).map(log => (
                    <tr key={log.id} className="clickable" onClick={() => setSelected(log)}>
                      <td className="dim mono">{fmtTime(log.timestamp)}</td>
                      <td>{log.eventType}</td>
                      <td><Badge value={log.status >= 400 ? 'FAILED' : log.eventType === 'LOGIN_SUCCESS' ? 'SUCCESS' : 'INFO'} />
                        <span className="mono dim" style={{ marginLeft: 6 }}>{log.status || ''}</span></td>
                      <td className="mono">{log.sourceIp}</td>
                      <td>{log.username}</td>
                      <td className="mono dim">{log.endpoint || `${log.protocol}/${log.port ?? ''}`}</td>
                      <td>{log.failedAttempts > 0 ? <span style={{ color: 'var(--orange)' }}>{log.failedAttempts}</span> : '0'}</td>
                      <td>{log.anomalyScore != null
                        ? <span style={{ color: log.anomalyScore >= .5 ? 'var(--red)' : 'var(--text-dim)' }}>
                            {log.anomalyScore.toFixed(2)}
                          </span>
                        : <span className="dim">—</span>}</td>
                      <td>›</td>
                    </tr>
                  ))}
                  {!data?.content?.length && (
                    <tr><td colSpan={9} className="empty">No logs found.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <Pagination page={page} size={data?.size || 50} total={data?.totalElements || 0} onPage={setPage} />
          </>
        )}
      </div>

      {selected && (
        <Drawer onClose={() => setSelected(null)}>
          <h2>Event Detail</h2>
          <p className="dim mono">{selected.eventId}</p>
          <dl className="kv">
            <dt>Timestamp</dt><dd>{fmtTime(selected.timestamp)}</dd>
            <dt>Event Type</dt><dd>{selected.eventType}</dd>
            <dt>Source IP</dt><dd className="mono">{selected.sourceIp}</dd>
            <dt>Destination</dt><dd className="mono">{selected.destinationIp || '—'}</dd>
            <dt>User</dt><dd>{selected.username || '—'}</dd>
            <dt>Origin</dt><dd>{selected.source || '—'}</dd>
            <dt>Endpoint</dt><dd className="mono">{selected.endpoint || '—'}</dd>
            <dt>HTTP</dt><dd>{[selected.httpMethod, selected.status].filter(Boolean).join(' · ') || '—'}</dd>
            <dt>Protocol</dt><dd>{selected.protocol} {selected.port ? `:${selected.port}` : ''}</dd>
            <dt>Bytes</dt><dd>{selected.bytes?.toLocaleString() ?? '—'}</dd>
            <dt>Failed Attempts</dt><dd>{selected.failedAttempts ?? 0}</dd>
            <dt>AI Anomaly Score</dt><dd>{selected.anomalyScore != null ? selected.anomalyScore : '—'}</dd>
            <dt>Classification</dt>
            <dd>{selected.attackType ? <Badge value={selected.attackType} /> : 'Benign'}
              {' '}{selected.severity && <Badge value={selected.severity} />}</dd>
          </dl>
          {selected.rawMessage && (
            <>
              <div className="section-title">Raw Message</div>
              <pre className="code">{selected.rawMessage}</pre>
            </>
          )}
        </Drawer>
      )}
    </>
  )
}
