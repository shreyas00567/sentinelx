import { useState } from 'react'
import { api, useFetch, Badge, fmtTime } from '../components/ui'

const ROLES = ['ADMIN', 'ANALYST', 'VIEWER']

export default function Users() {
  const { data: users, error, loading, reload } = useFetch(() => api('/api/users'), [])
  const [form, setForm] = useState({ username: '', email: '', password: '', fullName: '', role: 'ANALYST' })
  const [msg, setMsg] = useState(null)
  const [busy, setBusy] = useState(false)

  async function createUser(e) {
    e.preventDefault()
    setBusy(true); setMsg(null)
    try {
      await api('/api/users', { method: 'POST', body: form })
      setMsg({ ok: true, text: `User "${form.username}" created.` })
      setForm({ username: '', email: '', password: '', fullName: '', role: 'ANALYST' })
      reload()
    } catch (ex) { setMsg({ ok: false, text: ex.message }) }
    finally { setBusy(false) }
  }

  async function updateUser(u, patch) {
    setBusy(true)
    try {
      await api(`/api/users/${u.id}`, { method: 'PUT', body: patch })
      reload()
    } catch (ex) { alert(ex.message) }
    finally { setBusy(false) }
  }

  return (
    <>
      <div className="card" style={{ marginBottom: 20 }}>
        <h3>Create User</h3>
        {msg && <div className={msg.ok ? 'ok-box' : 'error-box'}>{msg.text}</div>}
        <form onSubmit={createUser} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: 10 }}>
          <input placeholder="Username" required minLength={3} value={form.username}
            onChange={e => setForm(f => ({ ...f, username: e.target.value }))} />
          <input type="email" placeholder="Email" required value={form.email}
            onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
          <input type="password" placeholder="Password (min 8 chars)" required minLength={8} value={form.password}
            onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
          <input placeholder="Full name" value={form.fullName}
            onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} />
          <select value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value }))}>
            {ROLES.map(r => <option key={r}>{r}</option>)}
          </select>
          <button className="btn" disabled={busy}>Create User</button>
        </form>
      </div>

      {error && <div className="error-box">{error}</div>}

      <div className="card">
        <h3>Existing Users</h3>
        {loading ? <div className="loading">Loading…</div> : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>User</th><th>Email</th><th>Role</th><th>Status</th><th>Created</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {(users || []).map(u => (
                  <tr key={u.id}>
                    <td style={{ fontWeight: 600 }}>{u.username}<div className="dim" style={{ fontSize: 12 }}>{u.fullName}</div></td>
                    <td className="dim">{u.email}</td>
                    <td>
                      <select value={u.role} disabled={busy}
                        onChange={e => updateUser(u, { role: e.target.value })}>
                        {ROLES.map(r => <option key={r}>{r}</option>)}
                      </select>
                    </td>
                    <td><Badge value={u.enabled ? 'SUCCESS' : 'FAILED'} />{' '}{u.enabled ? 'Active' : 'Disabled'}</td>
                    <td className="dim">{fmtTime(u.createdAt)}</td>
                    <td>
                      <button className={`btn sm ${u.enabled ? 'danger' : ''}`} disabled={busy}
                        onClick={() => updateUser(u, { enabled: !u.enabled })}>
                        {u.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  )
}
