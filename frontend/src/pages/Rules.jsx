import { useState } from 'react'
import { api, useFetch, Badge } from '../components/ui'
import { useAuth } from '../auth'

const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export default function Rules() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const { data: rules, error, loading, reload } = useFetch(() => api('/api/rules'), [])
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({})
  const [busy, setBusy] = useState(false)

  async function toggle(rule) {
    if (!isAdmin) return
    setBusy(true)
    try {
      await api(`/api/rules/${rule.id}`, { method: 'PUT', body: { enabled: !rule.enabled } })
      reload()
    } catch (ex) { alert(ex.message) }
    finally { setBusy(false); setEditingId(null) }
  }

  async function save(rule) {
    setBusy(true)
    try {
      await api(`/api/rules/${rule.id}`, {
        method: 'PUT',
        body: {
          threshold: form.threshold != null ? Number(form.threshold) : undefined,
          windowSeconds: form.windowSeconds != null ? Number(form.windowSeconds) : undefined,
          severity: form.severity
        }
      })
      setEditingId(null)
      reload()
    } catch (ex) { alert(ex.message) }
    finally { setBusy(false) }
  }

  function startEdit(r) {
    setEditingId(r.id)
    setForm({ threshold: r.threshold, windowSeconds: r.windowSeconds, severity: r.severity })
  }

  if (loading) return <div className="loading">Loading rules…</div>
  if (error) return <div className="error-box">{error}</div>

  return (
    <div className="card">
      <h3>Detection Rule Configuration {!isAdmin && <span className="dim" style={{ textTransform: 'none' }}>(read-only — admin required)</span>}</h3>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Rule</th><th>Description</th><th>Severity</th><th>Threshold</th><th>Window</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            {(rules || []).map(r => (
              <tr key={r.id}>
                <td style={{ fontWeight: 600 }}>{r.name}<div className="dim mono" style={{ fontSize: 11 }}>{r.ruleKey}</div></td>
                <td className="dim" style={{ maxWidth: 320 }}>{r.description}</td>
                <td>{editingId === r.id
                  ? <select value={form.severity} onChange={e => setForm(f => ({ ...f, severity: e.target.value }))}>
                      {SEVERITIES.map(s => <option key={s}>{s}</option>)}
                    </select>
                  : <Badge value={r.severity} />}</td>
                <td className="mono">{editingId === r.id
                  ? <input type="number" min={1} style={{ width: 80 }} value={form.threshold}
                      onChange={e => setForm(f => ({ ...f, threshold: e.target.value }))} />
                  : `≥ ${r.threshold}`}</td>
                <td className="mono">{editingId === r.id
                  ? <input type="number" min={10} max={3600} step={10} style={{ width: 90 }} value={form.windowSeconds}
                      onChange={e => setForm(f => ({ ...f, windowSeconds: e.target.value }))} />
                  : `${r.windowSeconds}s`}</td>
                <td>
                  <button className={`btn sm ${r.enabled ? '' : 'secondary'}`} disabled={!isAdmin || busy}
                    onClick={() => toggle(r)} title={isAdmin ? 'Click to toggle' : 'Admin only'}>
                    {r.enabled ? 'Enabled' : 'Disabled'}
                  </button>
                </td>
                <td>
                  {editingId === r.id
                    ? <span style={{ display: 'flex', gap: 6 }}>
                        <button className="btn sm" disabled={busy} onClick={() => save(r)}>Save</button>
                        <button className="btn sm secondary" onClick={() => setEditingId(null)}>Cancel</button>
                      </span>
                    : <button className="btn sm secondary" disabled={!isAdmin} onClick={() => startEdit(r)}>Edit</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
