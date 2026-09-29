import { Link } from 'react-router-dom'
import { api, useFetch, StatCard, Badge, RiskPill, BarChart, SevBars, HBarList, fmtTime } from '../components/ui'

export default function Dashboard() {
  const { data: s, error, loading } = useFetch(() => api('/api/dashboard/summary'), [])

  if (loading) return <div className="loading">Loading dashboard…</div>
  if (error) return <div className="error-box">{error}</div>

  const trend = s.alertsHourlyTrend || []

  return (
    <>
      <div className="grid-stats">
        <StatCard label="Total Alerts" value={s.alertsTotal} />
        <StatCard label="Alerts Today" value={s.alertsToday} tone="warn" />
        <StatCard label="AI Anomalies Today" value={s.aiAnomaliesToday} tone="danger" />
        <StatCard label="Open Incidents" value={s.openIncidents} tone="danger" />
      </div>

      <div className="grid-2">
        <div className="card">
          <h3>Alerts — Last 24 Hours</h3>
          <BarChart points={trend} />
        </div>
        <div className="card">
          <h3>Alerts by Severity</h3>
          <SevBars bySeverity={s.alertsBySeverity} />
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <h3>Attack Type Distribution</h3>
          <HBarList items={s.attackDistribution} />
        </div>
        <div className="card">
          <h3>Top Source IPs</h3>
          {!s.topSourceIps?.length ? <div className="empty">No data</div> :
            s.topSourceIps.slice(0, 8).map(ip => (
              <div key={ip.ip} className="hbar-row">
                <div className="hbar-label mono">{ip.ip}</div>
                <div className="hbar-track">
                  <div className="hbar-fill" style={{
                    width: `${(ip.count / Math.max(...s.topSourceIps.map(x => x.count), 1)) * 100}%`,
                    background: ip.maxRisk >= 70 ? 'linear-gradient(90deg,#ef4444,#f97316)' : undefined
                  }} />
                </div>
                <div className="hbar-val">{ip.count}</div>
              </div>
            ))}
        </div>
      </div>

      <div className="card">
        <h3>Recent Alerts</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Risk</th><th>Type</th><th>Detection</th><th>Severity</th>
                <th>Source IP</th><th>Target</th><th>Time</th>
              </tr>
            </thead>
            <tbody>
              {(s.recentAlerts || []).map(a => (
                <tr key={a.id}>
                  <td><RiskPill score={a.riskScore} /></td>
                  <td>{a.attackType}</td>
                  <td><Badge value={a.detectionMethod} /></td>
                  <td><Badge value={a.severity} /></td>
                  <td className="mono">{a.sourceIp}</td>
                  <td className="mono dim">{a.target}</td>
                  <td className="dim">{fmtTime(a.createdAt)}</td>
                </tr>
              ))}
              {!s.recentAlerts?.length && (
                <tr><td colSpan={7} className="empty">No alerts yet — run the Simulator to generate traffic.</td></tr>
              )}
            </tbody>
          </table>
        </div>
        <div className="pagination">
          <Link to="/alerts"><button className="btn sm secondary">View all alerts →</button></Link>
        </div>
      </div>
    </>
  )
}
