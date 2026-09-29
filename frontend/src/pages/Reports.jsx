import { useState } from 'react'
import { api, useFetch, HBarList, SevBars, StatCard } from '../components/ui'

export default function Reports() {
  const [days, setDays] = useState('7')
  const { data: r, error, loading } = useFetch(
    () => api('/api/reports/summary', { params: { days } }),
    [days]
  )

  if (loading) return <div className="loading">Generating report…</div>
  if (error) return <div className="error-box">{error}</div>

  return (
    <>
      <div className="toolbar">
        <label className="dim">Reporting window:&nbsp;</label>
        <select value={days} onChange={e => setDays(e.target.value)}>
          <option value="1">Last 24 hours</option>
          <option value="7">Last 7 days</option>
          <option value="30">Last 30 days</option>
          <option value="90">Last 90 days</option>
        </select>
        <div className="spacer" />
        <a href={`/api/reports/summary/download?days=${days}`} download>
          <button className="btn secondary">⬇ Download Report</button>
        </a>
      </div>

      <p className="dim" style={{ marginBottom: 16 }}>
        Generated {new Date(r.generatedAt).toLocaleString()} · window: {r.days} days
      </p>

      <div className="grid-stats" style={{ marginBottom: 20 }}>
        <StatCard label="Total Alerts" value={r.totalAlerts} />
        <StatCard label="Incidents Opened" value={r.incidentsOpened} tone="danger" />
        <StatCard label="Incidents Resolved" value={r.incidentsResolved} tone="ok" />
        <StatCard label="Avg Resolution (hrs)" value={r.avgResolutionHours?.toFixed(1) ?? '—'} />
        <StatCard label="AI Anomalies" value={r.aiAnomalies} tone="warn" />
      </div>

      <div className="grid-2">
        <div className="card"><h3>Alerts by Severity</h3><SevBars bySeverity={r.alertsBySeverity || {}} /></div>
        <div className="card"><h3>Alerts by Attack Type</h3><HBarList items={r.alertsByType} /></div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3>Top Source IPs</h3>
        <HBarList items={(r.topSourceIps || []).map(t => ({ type: t.ip, count: t.count }))} />
      </div>

      {r.recommendations?.length > 0 && (
        <div className="card">
          <h3>Recommendations</h3>
          <ul style={{ paddingLeft: 20, lineHeight: 2 }}>
            {r.recommendations.map((rec, i) => <li key={i}>{rec}</li>)}
          </ul>
        </div>
      )}
    </>
  )
}
