import { useState } from 'react'
import { api, useFetch, Badge, fmtTime } from '../components/ui'

export default function ThreatIntel() {
  const [ip, setIp] = useState('')
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const { data: recent } = useFetch(() => api('/api/threat-intel/recent'), [])

  async function lookup(e) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try { setResult(await api(`/api/threat-intel/ip/${encodeURIComponent(ip.trim())}`)) }
    catch (ex) { setError(ex.message) }
    finally { setBusy(false) }
  }

  return (
    <>
      <div className="toolbar">
        <form onSubmit={lookup} style={{ display: 'flex', gap: 10 }}>
          <input type="text" placeholder="Look up an IP address…" value={ip}
            onChange={e => setIp(e.target.value)} style={{ minWidth: 260 }} />
          <button className="btn" disabled={busy || !ip.trim()}>{busy ? 'Checking…' : 'Lookup'}</button>
        </form>
        <div className="spacer" />
      </div>

      {error && <div className="error-box">{error}</div>}

      {result && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h3>Reputation Report — <span className="mono">{result.ip || ip}</span></h3>
          <dl className="kv">
            <dt>Verdict</dt>
            <dd><Badge value={result.malicious ? 'OPEN' : 'SUCCESS'} />{' '}
              <b>{result.malicious ? 'MALICIOUS' : 'NO ABUSE REPORTED'}</b></dd>
            {result.confidenceScore != null && <><dt>Confidence</dt><dd>{result.confidenceScore}%</dd></>}
            {result.totalReports != null && <><dt>Abuse Reports</dt><dd>{result.totalReports}</dd></>}
            {result.isp && <><dt>ISP</dt><dd>{result.isp}</dd></>}
            {result.usageType && <><dt>Usage Type</dt><dd>{result.usageType}</dd></>}
            {result.countryCode && <><dt>Country</dt><dd>{result.countryCode}</dd></>}
            {result.provider && <><dt>Data Source</dt><dd>{result.provider}</dd></>}
          </dl>
          {result.queriedAt && <p className="dim">Last checked: {fmtTime(result.queriedAt)}</p>}
        </div>
      )}

      <div className="card">
        <h3>Recently Flagged IPs</h3>
        <div className="table-wrap">
          <table>
            <thead><tr><th>IP</th><th>Verdict</th><th>Confidence</th><th>Reports</th><th>Source</th><th>Checked</th></tr></thead>
            <tbody>
              {(recent || []).map((r, i) => (
                <tr key={i}>
                  <td className="mono">{r.ip}</td>
                  <td><Badge value={r.malicious ? 'OPEN' : 'SUCCESS'} /></td>
                  <td>{r.confidenceScore != null ? `${r.confidenceScore}%` : '—'}</td>
                  <td>{r.totalReports ?? '—'}</td>
                  <td>{r.provider || '—'}</td>
                  <td className="dim">{fmtTime(r.queriedAt)}</td>
                </tr>
              ))}
              {!recent?.length && <tr><td colSpan={6} className="empty">No lookups recorded yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  )
}
