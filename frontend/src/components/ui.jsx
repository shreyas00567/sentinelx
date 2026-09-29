import { useEffect, useState } from 'react'
import { api } from '../api'

export function StatCard({ label, value, delta, tone }) {
  const color = tone === 'danger' ? 'var(--red)' : tone === 'warn' ? 'var(--yellow)' : tone === 'ok' ? 'var(--green)' : 'var(--accent)'
  return (
    <div className="card stat">
      <span className="num" style={{ color }}>{value ?? '—'}</span>
      <span className="label">{label}</span>
      {delta && <span className="delta">{delta}</span>}
    </div>
  )
}

export function Badge({ value }) {
  if (!value) return null
  return <span className={`badge ${String(value).toUpperCase().replace(/\s+/g, '_')}`}>{value}</span>
}

export function RiskPill({ score }) {
  const cls = score >= 70 ? 'risk-high' : score >= 40 ? 'risk-med' : 'risk-low'
  return <span className={`risk-pill ${cls}`}>{score}</span>
}

export function BarChart({ points, height = 120 }) {
  if (!points?.length) return <div className="empty">No data</div>
  const max = Math.max(...points.map(p => p.count), 1)
  return (
    <div className="chart-row" style={{ height }}>
      {points.map((p, i) => (
        <div key={i} className="bar-col" title={`${p.label}: ${p.count}`}>
          <div className="bar" style={{ height: `${(p.count / max) * 100}%` }} />
          <div className="bar-label">{p.label}</div>
        </div>
      ))}
    </div>
  )
}

const SEV_COLORS = {
  CRITICAL: 'var(--red)', HIGH: 'var(--orange)', MEDIUM: 'var(--yellow)',
  LOW: 'var(--accent)', INFO: '#64748b'
}

export function SevBars({ bySeverity = {} }) {
  const rows = Object.entries(bySeverity)
  if (!rows.length) return <div className="empty">No data</div>
  const max = Math.max(...rows.map(([, v]) => v), 1)
  const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']
  rows.sort((a, b) => order.indexOf(a[0]) - order.indexOf(b[0]))
  return (
    <>
      {rows.map(([sev, count]) => (
        <div key={sev} className="hbar-row">
          <div className="hbar-label"><span className={`sev-dot dot-${sev}`} />{sev}</div>
          <div className="hbar-track">
            <div className="hbar-fill" style={{ width: `${(count / max) * 100}%`, background: SEV_COLORS[sev] || 'var(--accent)' }} />
          </div>
          <div className="hbar-val">{count}</div>
        </div>
      ))}
    </>
  )
}

export function HBarList({ items, labelKey = 'type', valueKey = 'count' }) {
  if (!items?.length) return <div className="empty">No data</div>
  const max = Math.max(...items.map(i => i[valueKey]), 1)
  return (
    <>
      {items.slice(0, 8).map(item => (
        <div key={item[labelKey]} className="hbar-row">
          <div className="hbar-label">{item[labelKey]}</div>
          <div className="hbar-track">
            <div className="hbar-fill" style={{ width: `${(item[valueKey] / max) * 100}%` }} />
          </div>
          <div className="hbar-val">{item[valueKey]}</div>
        </div>
      ))}
    </>
  )
}

export function Pagination({ page, size, total, onPage }) {
  const totalPages = Math.max(1, Math.ceil(total / size))
  return (
    <div className="pagination">
      <span>{total} results · page {page + 1}/{totalPages}</span>
      <button className="btn sm secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>Prev</button>
      <button className="btn sm secondary" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
    </div>
  )
}

export function Drawer({ onClose, children }) {
  useEffect(() => {
    const onKey = e => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])
  return (
    <div className="drawer-overlay" onClick={e => e.target === e.currentTarget && onClose()}>
      <aside className="drawer">{children}</aside>
    </div>
  )
}

export function fmtTime(ts) {
  if (!ts) return '—'
  return new Date(ts).toLocaleString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

export function useFetch(fn, deps) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try { setData(await fn()) }
    catch (ex) { setError(ex.message) }
    finally { setLoading(false) }
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load() }, deps)

  return { data, error, loading, reload: load }
}

export { api }
