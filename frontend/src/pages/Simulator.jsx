import { useState } from 'react'
import { api, Badge } from '../components/ui'

const SCENARIOS = [
  { key: 'normal-login', name: 'Normal Login', desc: 'Single benign login event. Baseline traffic — no alert expected.', tone: '#22c55e' },
  { key: 'brute-force', name: 'Brute Force', desc: '8 failed logins across user accounts from one IP. Triggers the brute-force rate rule.', tone: '#f97316' },
  { key: 'sql-injection', name: 'SQL Injection', desc: "UNION SELECT and OR 1=1 payloads against product/search endpoints.", tone: '#ef4444' },
  { key: 'xss', name: 'XSS Attempt', desc: 'Stored <script> payload posted to a comments endpoint.', tone: '#eab308' },
  { key: 'ddos', name: 'DDoS Burst', desc: '110 rapid HTTP requests from a single IP. Trips the DDoS rate rule.', tone: '#ef4444' },
  { key: 'port-scan', name: 'Port Scan', desc: 'Sequential TCP connection attempts across many ports.', tone: '#f97316' },
  { key: 'insider-anomaly', name: 'Insider Anomaly', desc: 'Legitimate credentials used at an odd hour with massive file exports. Designed to trigger the ML autoencoder.', tone: '#a78bfa' }
]

export default function Simulator() {
  const [running, setRunning] = useState(null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  async function run(key) {
    setRunning(key)
    setError(null)
    setResult(null)
    try {
      const r = await api(`/api/simulator/${key}`, { method: 'POST' })
      setResult(r)
    } catch (ex) { setError(ex.message) }
    finally { setRunning(null) }
  }

  return (
    <>
      <p className="dim" style={{ marginBottom: 16 }}>
        Replay realistic attack scenarios through the full pipeline: ingestion → rule engine → ML anomaly
        detection → risk scoring → alerts &amp; incidents. Check <b>Alerts</b> and <b>Incidents</b> after running.
      </p>

      <div className="scenario-grid">
        {SCENARIOS.map(s => (
          <div key={s.key} className="scenario-card" onClick={() => !running && run(s.key)}>
            <h4 style={{ color: s.tone }}>{s.name}</h4>
            <p>{s.desc}</p>
            <div style={{ marginTop: 12 }}>
              <button className="btn sm" disabled={!!running}>
                {running === s.key ? 'Running…' : '▶ Run'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {error && <div className="error-box" style={{ marginTop: 18 }}>{error}</div>}

      {result && (
        <div className="card" style={{ marginTop: 18 }}>
          <h3>Result — {result.scenario}</h3>
          <div className="grid-stats" style={{ marginBottom: 14 }}>
            <div className="stat"><span className="num">{result.eventsGenerated}</span><span className="label">Events Generated</span></div>
            <div className="stat"><span className="num" style={{ color: result.alerts?.length ? 'var(--red)' : 'var(--green)' }}>{result.alerts?.length || 0}</span><span className="label">Alerts Raised</span></div>
            <div className="stat"><span className="num" style={{ color: result.incidentIds?.length ? 'var(--orange)' : 'var(--text)' }}>{result.incidentIds?.length || 0}</span><span className="label">Incidents Opened</span></div>
            <div className="stat"><span className="num" style={{ color: result.aiAnomalies ? 'var(--purple)' : 'var(--text)' }}>{result.aiAnomalies}</span><span className="label">AI Anomalies</span></div>
          </div>
          {result.alerts?.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Type</th><th>Detection</th><th>Severity</th><th>Risk</th></tr></thead>
                <tbody>
                  {result.alerts.map((a, i) => (
                    <tr key={i}>
                      <td>{a.attackType}</td>
                      <td><Badge value={a.method} /></td>
                      <td><Badge value={a.severity} /></td>
                      <td>{a.riskScore}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {!result.alerts?.length && (
            <p className="ok-box" style={{ marginTop: 6 }}>Scenario completed with zero detections — as expected for benign traffic.</p>
          )}
        </div>
      )}
    </>
  )
}
